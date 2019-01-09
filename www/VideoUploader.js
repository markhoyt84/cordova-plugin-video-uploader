//
//  VideoUploader.js
//
//  Created by Mark Hoyt
//

var exec = require('cordova/exec');
var pluginName = 'VideoUploader';

var VideoUploader = {

    upload: function(successCallback, errorCallback, args) {
        var argsArray = [];
        if(args){
            argsArray.push(args);
        }
        exec(successCallback, errorCallback, pluginName, "upload", argsArray);
    }
}

module.exports = VideoUploader;
