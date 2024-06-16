import { WebPlugin } from '@capacitor/core';

import type { ThermalPrinterPlugin } from './definitions';

export class ThermalPrinterWeb extends WebPlugin implements ThermalPrinterPlugin {
  async printFormattedText(): Promise<void> {
    console.log('Text print');
    return;
  }

  async requestPermissions(): Promise<void> {
    console.log('Web does not require permissions');
    return;
  }

  async listPrinters(): Promise<{ printers: any[] }> {
    const printers = [
      {
        productName: 'Simulated Printer 1',
        manufacturerName: 'Printer Manufacturer 1',
        deviceId: '12345',
        serialNumber: 'SN12345',
        vendorId: 'VEND1',
      },
      {
        productName: 'Simulated Printer 2',
        manufacturerName: 'Printer Manufacturer 2',
        deviceId: '67890',
        serialNumber: 'SN67890',
        vendorId: 'VEND2',
      }
    ];

    return { printers };
  }

  async launchIntent(): Promise<void> {
    console.log('Text print');
    return;
  }

}
