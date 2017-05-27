# JingYanGeofencing
A Geofencing app that monitors a user's location and paints it onto an interactive map.

##### First of all, please change the google_maps_key (google_maps_api.xml, MapsActivity.java) to a valid Google Place API key (no restriction) to run the application.
----------------------------------

Approach 

This GEO-FENCE application was approached through the following steps:

1. Start from Google Maps Activity template, set API key.

2. Set google map’s camera in onMapReady(), add blue marker on the current position. 

3. Set location listener to get location update, set current location to blue marker, and draw path.

4. Send request to query nearby points of interest from Google Place API Web Service. 

5. Parse Json objects to a list of place results (with name, place_id, latitude, longitude), add marker to corresponding position, set title.

6. Override onInfoWindowClick() function to get a place_id of a target marker, send request to query detail place information from Google API, and get google map web URL. 

7. Embedded the google map web page in application with a WebView object, start an intent with the WebViewActivity class, and set a webViewClient in the class.

8. Add GeoFence around the POIs, draw GeoFence around markers, create notification in GeoFenceTransitionService class. (The GeoFence part is a bit complicated with lots of setting procedure)

9. Handling orientation changes and save the state of map’s camera.

---------------------------

References

1. Google Place API
https://developers.google.com/places/web-service/search
https://developers.google.com/places/web-service/details

2. Parse Json
https://www.tutorialspoint.com/android/android_json_parser.htm

3. GeoFence API & Example
https://developer.android.com/training/location/geofencing.html
https://code.tutsplus.com/tutorials/how-to-work-with-geofences-on-android--cms-26639 

4. Notification
https://developer.android.com/guide/topics/ui/notifiers/notifications.html



