# Instructions to install Smartenit device on SmartThings.

1. Login to SmartThings IDE at https://graph.api.smartthings.com (same login as SmartThings App)
2. Click on "My Device Handlers"
3. Click on "Create New Device Handler"
4. Click on "From Code" and then you will see a big text box to paste the device handler code.
5. Open a different tab and navigate to https://github.com/thewall7/st-devicehandlers
    1. Shortcut for ZBMPlug15 - https://raw.githubusercontent.com/thewall7/st-devicehandlers/master/smartenit-zbmplug15.src/smartenit-zbmplug15.groovy
    2. Shortcut for ZBMLC30 - https://raw.githubusercontent.com/thewall7/st-devicehandlers/master/smartenit-zbmlc30.src/smartenit-zbmlc30.groovy
    3. Shortcut for ZBMSKT - https://raw.githubusercontent.com/thewall7/st-devicehandlers/master/smartenit-zbmskt.src/smartenit-zbmskt.groovy
    4. Longcut - Click on the desired device directory and you will see a groovy file. Click to open that file. You will see all the device handler code. Click on "Raw" button to see the code properly.  Copy this code exactly the way it is.
6. Paste the code from previous step in the SmartThings Text box, then click Create at the bottom of the page. The device handler will get created and you will see few options on top right.
7. Click "Save", click "Publish", and then click "For Me".  It should say "Device type published successfully."
8. Now go to the Smartthings App and click "Add a Thing"
9. Factory Reset the Device (refer to the device manual for procedure)
10. Wait for Device to Join the SmartThings Hub. When the device joins the Smartthings hub you should see the same name as the Device Handler. Rename the device to something meaningful, such as "Water Heater", save and then click Ok to confirm joined device. 
11. Now you should be able to read the status of the device and control it.

If you have any issues, contact us at support@smartenit.com

