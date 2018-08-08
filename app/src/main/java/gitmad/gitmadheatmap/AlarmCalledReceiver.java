package gitmad.gitmadheatmap;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class AlarmCalledReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent ) {
        // If there is no connection we do not want to send off a request to get user location data
        // Often the task will hang for a while and will stack if they are not stopped. This is one way to stop queuing tasks.
        if( isNetworkAvailable( context ) ) {
            updateUserLocation( context );
        }
    }

    /**
     * Checks to see if the user has a current internet connection.
     * @param context Current context.
     * @return true if the user has an internet connection, false otherwise.
     */
    private boolean isNetworkAvailable( Context context ) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService( Context.CONNECTIVITY_SERVICE );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Create a task handler that gets the users current location and then uploads it to the database.
     * @param context Current context
     */
    private void updateUserLocation(final Context context ) {
        // If the user does not grant location permissions then their information will not be uploaded.
        if(ContextCompat.checkSelfPermission( context, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
            FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

            // Create new task promise.
            final Task locationResult = mFusedLocationProviderClient.getLastLocation();

            locationResult.addOnCompleteListener( new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        // Send the user's location information to our database.
                        Location mLastKnownLocation;
                        mLastKnownLocation = (Location) task.getResult();
                        LatLng mCoordinates = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                        FbDatabase mDatabase = new FbDatabase();

                        // Get the user's username.
                        String username = retrieveUsername();
                        mDatabase.addLocation( new LocationInformation( mCoordinates, username ) );
                    }
                }
            });
        }
    }

    /**
     * Retrieve identification of the current user.
     * @return the user's username if they are logged in or their userID if they are logged out.
     */
    private String retrieveUsername() {
        SharedPreferences sharedPreferences = MyApp.getContext().getSharedPreferences( MyApp.getContext().getString( R.string.pref_preferences ), Context.MODE_PRIVATE );
        String userId = sharedPreferences.getString( MyApp.getContext().getString( R.string.pref_user_username), null );

        if( userId != null ) {
            return userId;
        }

        userId = sharedPreferences.getString( MyApp.getContext().getString( R.string.pref_user_id ), null );

        return userId;
    }
}
