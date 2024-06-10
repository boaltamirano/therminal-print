// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "ThermalPrinter",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "ThermalPrinter",
            targets: ["ThermalPrinterPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "ThermalPrinterPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/ThermalPrinterPlugin"),
        .testTarget(
            name: "ThermalPrinterPluginTests",
            dependencies: ["ThermalPrinterPlugin"],
            path: "ios/Tests/ThermalPrinterPluginTests")
    ]
)