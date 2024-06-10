import { WebPlugin } from '@capacitor/core';

import type { ThermalPrinterPlugin } from './definitions';

export class ThermalPrinterWeb
  extends WebPlugin
  implements ThermalPrinterPlugin
{
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
