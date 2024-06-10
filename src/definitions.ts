export interface ThermalPrinterPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
