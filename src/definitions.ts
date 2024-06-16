export interface PrinterToUse {
  id: string | number;
}

export interface PrintFormattedText extends PrinterToUse {
  text: string;
}

export interface Intent {
  param: string;
}

export interface ThermalPrinterPlugin {
  printFormattedText(data:PrintFormattedText ): Promise<void>;
  requestPermissions(data: PrinterToUse): Promise<void>;
  listPrinters(): Promise<{ printers: any[] }>;
  launchIntent(data: Intent): Promise<void>;
}
