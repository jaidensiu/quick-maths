//
//  NetworkMonitor.swift
//  QuickMaths
//
//  Created by Jaiden Siu on 2026-10-12.
//

import Network

/// Reports internet reachability changes, like Android's `NetworkMonitor`.
final class NetworkMonitor {
    private let monitor = NWPathMonitor()
    private var onChange: (_ isOnline: Bool) -> Void = { _ in }
    private var lastIsOnline: Bool?

    /// Calls `onChange` on the main thread with the first known state, then on every change.
    func start(onChange: @escaping (_ isOnline: Bool) -> Void) {
        self.onChange = onChange
        monitor.pathUpdateHandler = { [weak self] path in
            let isOnline = path.status == .satisfied
            MainActor.assumeIsolated {
                self?.update(isOnline: isOnline)
            }
        }
        monitor.start(queue: .main)
    }

    private func update(isOnline: Bool) {
        guard isOnline != lastIsOnline else { return }
        lastIsOnline = isOnline
        onChange(isOnline)
    }

    deinit {
        monitor.cancel()
    }
}
