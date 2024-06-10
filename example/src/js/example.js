import { ThermalPrinter } from 'thermal-printer';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    ThermalPrinter.echo({ value: inputValue })
}
