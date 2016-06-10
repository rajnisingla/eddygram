
var CONF_SERVICE_UUID = "ee0c2080-8786-40ba-ab96-99b91ac981d8";
var EDDYSTONE_URL_SERVICE_UUID = "0000fed8-0000-1000-8000-00805f9b34fb";
var URI_DATA_CHARACTERISTIC_UUID = "ee0c2080-8786-40ba-ab96-99b91ac981d8";

function onChooseClick() {
  'use strict';

  console.log('Requesting Bluetooth Device...');
  navigator.bluetooth.requestDevice(
    {filters: [{services: ["0000fed8-0000-1000-8000-00805f9b34fb"]}],optionalServices: ["ee0c2080-8786-40ba-ab96-99b91ac981d8"]})
  .then(device => {
    console.log('> Found ' + device.name);
    console.log('Connecting to GATT Server...');
    if(devicesList.indexOf(device)>-1)
    devicesList.push(device);
    //return device.gatt.connect();
  })
  .then(server => {
    console.log('Getting Config Service...');
    return server.getPrimaryService('ee0c2080-8786-40ba-ab96-99b91ac981d8');
  })
  .then(service => {
    console.log('Getting URI Data Characteristic...');
    return service.getCharacteristic('ee0c2084-8786-40ba-ab96-99b91ac981d8');
  })
  .then(ch => {
	  console.log('Characteristic assigned...');
	  characteristic = ch;
  })
  .catch(error => {
    console.log('Argh! ' + error);
  });
}


function onWrite(url){

	console.log('Writing value...==> '+url);
	console.log(characteristic);
   let xyz = text2ua(url);
   characteristic.writeValue(xyz);
}

function text2ua(s) {
    var ua = new Uint8Array(s.length+1);
	ua[0] = 3 ;
    for (var i = 0; i < s.length; i++) {
        ua[i+1] = s.charCodeAt(i);
    }
    return ua;
}
