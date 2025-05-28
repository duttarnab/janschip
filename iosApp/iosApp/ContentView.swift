import SwiftUI
import shared

enum Screen {
    case discovery
    case login
    case userInfo(String)
}

struct ContentView: View {
    @State private var discoveryUrl = ""
    @State private var username = ""
    @State private var password = ""
    @State private var currentScreen: Screen = .discovery
    @State private var toastMessage: String?

    let settings = IosSettingsProvider().getSettings()
    var discoveryService: DiscoveryService
    var authService: AuthService
    var logoutService: LogoutService

    init() {
        self.discoveryService = DiscoveryService(settings: settings)
        self.authService = AuthService(settings: settings)
        self.logoutService = LogoutService(settings: settings)

        // Check for stored config on init
        //Task {
            let stored = discoveryService.isConfigStored()
            //DispatchQueue.main.async {
                if stored {
                    currentScreen = .login
                }
            //}
        //}
    }

    var body: some View {
        VStack(spacing: 16) {
            switch currentScreen {
            case .discovery:
                TextField("Enter OpenID Provider URL", text: $discoveryUrl)
                    .textFieldStyle(.roundedBorder)
                    .autocapitalization(.none)
                Button("Submit") {
                    Task {
                        do {
                            let result = try await discoveryService.fetchConfigAndRegisterClient(discoveryUrl: discoveryUrl)
                            if result.success {
                                currentScreen = .login
                                toastMessage = "Config saved"
                            } else {
                                toastMessage = result.errorMessage ?? "Failed to fetch config"
                            }
                        } catch {
                                toastMessage = "Unexpected error: \(error.localizedDescription)"
                        }
                    }
                }
            case .login:
                Text("Login").font(.headline)
                TextField("Username", text: $username)
                    .textFieldStyle(.roundedBorder)
                    .autocapitalization(.none)
                SecureField("Password", text: $password)
                    .textFieldStyle(.roundedBorder)
                    .autocapitalization(.none)
                Button("Login") {
                    Task {
                        toastMessage = "Login attempted"
                        do {
                            let result = try await authService.authenticate(username: username, password: password)
                            if result.success {
                                currentScreen = .userInfo(result.message ?? "")
                            } else {
                                toastMessage = result.message ?? "Login failed"
                            }
                        } catch {
                                toastMessage = "Unexpected error: \(error.localizedDescription)"
                        }
                    }
                }
                Button("Reset Config") {
                    discoveryService.clearConfig()
                    discoveryUrl = ""
                    currentScreen = .discovery
                    toastMessage = "Config reset"
                }
            case .userInfo(let info):
                Text("User Info").font(.headline)
                ScrollView {
                    Text(info).padding()
                }
                Button("Logout") {
                    Task {
                        do {
                            _ = try await logoutService.logout()
                            currentScreen = .login
                            toastMessage = "Session cleared"
                        } catch {
                                toastMessage = "Unexpected error: \(error.localizedDescription)"
                        }
                    }
                }
            }

            if let toast = toastMessage {
                Text(toast)
                    .foregroundColor(.white)
                    .padding(8)
                    .background(Color.black.opacity(0.7))
                    .cornerRadius(8)
                    .transition(.opacity)
            }

            Spacer()
        }
        .padding()
        .animation(.default, value: toastMessage)
    }
}

