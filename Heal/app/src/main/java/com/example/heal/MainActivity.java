    package com.example.heal; // Replace this!


    import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;

    import static java.security.AccessController.getContext;

    import android.content.Intent;
    import android.graphics.Color;
    import android.graphics.drawable.ColorDrawable;
    import android.net.Uri;
    import android.os.Bundle;
    import android.os.Handler;
    import android.util.Log;
    import android.view.Gravity;
    import android.view.LayoutInflater;
    import android.view.MenuItem;
    import android.view.View;
    import android.view.ViewGroup;
    import android.view.ViewTreeObserver;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.FrameLayout;
    import android.widget.ImageButton;
    import android.widget.LinearLayout;
    import android.widget.PopupWindow;
    import android.widget.ScrollView;
    import android.widget.TextView;
    import android.widget.Toast;
    import androidx.annotation.NonNull;
    import androidx.appcompat.app.ActionBarDrawerToggle;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.appcompat.widget.Toolbar;
    import androidx.constraintlayout.widget.ConstraintLayout;
    import androidx.coordinatorlayout.widget.CoordinatorLayout;
    import androidx.core.view.GravityCompat;
    import androidx.drawerlayout.widget.DrawerLayout;
    import androidx.fragment.app.Fragment;
    import androidx.fragment.app.FragmentManager;
    import androidx.fragment.app.FragmentTransaction;

    import com.google.android.material.bottomsheet.BottomSheetBehavior;
    import com.google.android.material.navigation.NavigationView;
    import com.google.android.material.textfield.TextInputEditText;
    import com.google.android.material.textfield.TextInputLayout;
    // Import the correct binding!

    import java.util.HashMap;

    import ui.GalleryFragment;
    import ui.HomeFragment;
    import ui.Loading;
    import ui.SettingsFragment;
    import ui.SlideshowFragment;

    public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

        private DrawerLayout drawerLayout;

        private com.google.android.material.floatingactionbutton.FloatingActionButton Fab;
        private Toolbar toolbar;
        private NavigationView navigationView;
        private MenuItem previousMenuItem;
        private View previousItemView;
        private ImageButton MenuTrigger;
        private PopupWindow popupWindow; // Declare PopupWindow as a class member

        // Bottom Sheet related variables
        FrameLayout bottomSheetContent;

        private static final String TAG = "YourActivityTag";
        View bottomSheetView;
        BottomSheetBehavior<View> bottomSheetBehavior;// Use the settings binding!

        // Overlay for graying out
        private View overlayView;

        private View fragmentMain;

        private TextInputEditText RecipientId;

        private TextInputEditText SendMessage;

        private Button sendMessage;
        private Button cancelMessage;
        private HashMap<String, String> sendData = new HashMap<>();

        private  EditText recipientEditText; // Replace with your actual ID
       private EditText messageEditText;

        private final String myAppLink = "https://play.google.com/store/apps/details?id=com.example.myapp";
       private final String shareMessage = "Check out this awesome app!";


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);

            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            // Load the Home Room when the app starts
            if (savedInstanceState == null) {
                loadFragment(new Loading()); // Load the loading fragment
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadFragment(new HomeFragment()); // Load the HomeFragment after a delay
                    }
                }, 2000);
                navigationView.setCheckedItem(R.id.nav_home);
                toolbar.setTitle("Home Room");
            }

            // Initialize Bottom Sheet views and behavior
            bottomSheetContent = findViewById(R.id.bottom_sheet_content);
            bottomSheetView = findViewById(R.id.bottom_sheet_container);
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
            bottomSheetBehavior.setState(STATE_COLLAPSED); // Initial state



//            if (Fab != null) {
//                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) Fab.getLayoutParams();
//               if (params.bottomMargin >0 || params.bottomMargin == 16) { // Check if bottomMargin is 0
//                   params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin + (int) getResources().getDimension(R.dimen.fab_bottom_margin));
//                   Fab.setLayoutParams(params);
//               }
//            }

            MenuTrigger = findViewById(R.id.menu_trigger);
            MenuTrigger.setOnClickListener(v -> {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    Fab = findViewById(R.id.fab);
                    if (Fab != null) {
                        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) Fab.getLayoutParams();
                        if (params.bottomMargin >0) { // Check if bottomMargin is 0
                            if (params.bottomMargin  == (int) getResources().getDimension(R.dimen.fab_default_margin)) {
                                clearBottomFragment();
                                toggleBottomSheet();
                                params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin + (int) getResources().getDimension(R.dimen.fab_null_margin));
                                Fab.setLayoutParams(params);
                                Log.d(TAG, "onBackPressed: state collapsed and FAB margin adjusted");

                            }else {

                            }
                        }
                        else {
                            Log.d(TAG, "onBackPressed: state expanded, but FAB margin is not 0");
                        }
                    }
                }
                // Inflate the custom menu layout for the popup
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.custom_menu_layout, null);
                

                // Create the PopupWindow
                popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);

                // Find the Settings TextView in the popup
                TextView settings = popupView.findViewById(R.id.menu_item_1);
                settings.setOnClickListener(v1 -> {
                    bottomSheetBehavior.setState(STATE_COLLAPSED);
                    Fab = findViewById(R.id.fab);
                    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) Fab.getLayoutParams();
                    Log.d(TAG, "Settings clicked. Bottom Sheet State: " + bottomSheetBehavior.getState()); // Add this line
                    // When Settings is clicked in the popup:
                    // 1. Load the SettingsFragment into the bottom sheet container
                    if (bottomSheetBehavior.getState() == STATE_COLLAPSED) {
                        loadBottomFragment(new SettingsFragment());
                        toggleBottomSheet();
                        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin + (int) getResources().getDimension(R.dimen.fab_bottom_margin));
                        Fab.setLayoutParams(params);
                    }
                    popupWindow.dismiss();
                });

                // Find the Logout TextView in the popup
                TextView logout = popupView.findViewById(R.id.menu_item_2);
                logout.setOnClickListener(view -> {
                    // Add your logout functionality here
                    Toast.makeText(MainActivity.this, "Logout Clicked", Toast.LENGTH_SHORT).show();
                    popupWindow.dismiss();
                });

                // Show the popup window anchored to the MenuTrigger

                popupWindow.showAtLocation(v, Gravity.END, 30, -700);
            });
            //settings collapsed when toolbar clicked
            if (toolbar != null) {
                toolbar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                            Fab = findViewById(R.id.fab);
                            if (Fab != null) {
                                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) Fab.getLayoutParams();
                                if (params.bottomMargin >0) { // Check if bottomMargin is 0
                                    if (params.bottomMargin  == (int) getResources().getDimension(R.dimen.fab_default_margin)) {
                                        clearBottomFragment();
                                        toggleBottomSheet();
                                        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin + (int) getResources().getDimension(R.dimen.fab_null_margin));
                                        Fab.setLayoutParams(params);
                                        Log.d(TAG, "onBackPressed: state collapsed and FAB margin adjusted");

                                    }else {

                                    }
                                }
                                else {
                                    Log.d(TAG, "onBackPressed: state expanded, but FAB margin is not 0");
                                }
                            }
                        }
                    }
                });
            }
            //settings collapsed when fragment clicked
            fragmentMain = findViewById(R.id.fragment_container);
//            fragmentMain.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
//                        Fab = findViewById(R.id.fab);
//                        if (Fab != null) {
//                            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) Fab.getLayoutParams();
//                            if (params.bottomMargin >0) { // Check if bottomMargin is 0
//                                clearBottomFragment();
//                                toggleBottomSheet();
//                                params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin + (int) getResources().getDimension(R.dimen.fab_null_margin));
//                                Fab.setLayoutParams(params);
//                                Log.d(TAG, "onBackPressed: state collapsed and FAB margin adjusted");
//                            }
//                            else {
//                                Log.d(TAG, "onBackPressed: state expanded, but FAB margin is not 0");
//                                // Optionally, add a Toast or other feedback if the margin is not 0
//                            }
//                        }
//                    }
//                }
//            });
    // Settings collapsed when drawer opened
            if (drawerLayout != null) {
                drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
                    @Override
                    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                        // Perform action when the drawer slides
                    }
                    @Override
                    public void onDrawerOpened(@NonNull View drawerView) {
                        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                            Fab = findViewById(R.id.fab);
                            if (Fab != null) {
                                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) Fab.getLayoutParams();
                                if (params.bottomMargin >0) { // Check if bottomMargin is 0
                                    if (params.bottomMargin  == (int) getResources().getDimension(R.dimen.fab_default_margin)) {
                                        clearBottomFragment();
                                        toggleBottomSheet();
                                        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin + (int) getResources().getDimension(R.dimen.fab_null_margin));
                                        Fab.setLayoutParams(params);
                                        Log.d(TAG, "onBackPressed: state collapsed and FAB margin adjusted");

                                    }else {

                                    }
                                }
                                else {
                                    Log.d(TAG, "onBackPressed: state expanded, but FAB margin is not 0");
                                }
                            }
                        }
                        // Perform action when the drawer opens
                    }
                    @Override
                    public void onDrawerClosed(@NonNull View drawerView) {
                        // Perform action when the drawer closes
                    }
                    @Override
                    public void onDrawerStateChanged(int newState) {
                        // Perform action when the drawer state changes
                    }
                });
            }


        }


        private View findNavigationViewItemView(@NonNull MenuItem item) {
            if (navigationView == null) return null;
            return navigationView.findViewById(item.getItemId());
        }



        @Override
        public void onBackPressed() {
            // If the drawer is open, close it
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return; // Consume the back press
            }
            // If the bottom sheet is expanded, collapse it and hide the overlay
            else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                clearBottomFragment();
                toggleBottomSheet();
                Fab = findViewById(R.id.fab);
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) Fab.getLayoutParams();
                if (params.bottomMargin > 16) {
                    clearBottomFragment();
                    toggleBottomSheet();
                    params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin + (int) getResources().getDimension(R.dimen.fab_null_margin));
                    Fab.setLayoutParams(params);
                    Log.d(TAG, "onBackPressed: state collapsed and FAB margin adjusted");
                }
                Log.d(TAG, "onBackPressed: state collapsed");
                return; // Consume the back press
            }
            // If the popup window is showing, dismiss it
            else if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss();
                return;
            }
            // Otherwise, perform the default back button behavior
            super.onBackPressed();
        }



        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            int id = item.getItemId();
            View currentItemView = navigationView.findViewById(id);

            // Remove background color from the previously selected item
            if (previousItemView != null && previousItemView != currentItemView) {
                previousItemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }

            if (id == R.id.nav_home) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(currentFragment instanceof HomeFragment)) {
                    loadFragment(new Loading()); // Load the loading fragment
                    new Handler().postDelayed(() -> loadFragment(new HomeFragment()), 2000);
                }
                navigationView.setCheckedItem(R.id.nav_home);
                toolbar.setTitle("Heal");
            } else if (id == R.id.nav_gallery) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(currentFragment instanceof GalleryFragment)) {
                    loadFragment(new Loading()); // Load the loading fragment
                    new Handler().postDelayed(() -> loadFragment(new GalleryFragment()), 2000);
                }
                navigationView.setCheckedItem(R.id.nav_gallery);
                toolbar.setTitle("Art Corner");
            } else if (id == R.id.nav_slideshow) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(currentFragment instanceof SlideshowFragment)) {
                    loadFragment(new Loading()); // Load the loading fragment
                    new Handler().postDelayed(() -> loadFragment(new SlideshowFragment()), 2000);
                }
                navigationView.setCheckedItem(R.id.nav_slideshow);
                toolbar.setTitle("Game Room");
            } else if (id == R.id.nav_send) {
                // Always set Home as checked when navigating to "Send"
                navigationView.setCheckedItem(R.id.nav_home);
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(currentFragment instanceof HomeFragment)) {
                    loadFragment(new Loading()); // Load the loading fragment
                    new Handler().postDelayed(() -> {
                        loadFragment(new HomeFragment());
                        showSendPopup();
                    }, 2000);
                } else {
                    showSendPopup();
                }
                toolbar.setTitle("Heal");

            } else if (id == R.id.nav_share) {
                // Always set Home as checked when navigating to "Share"
                navigationView.setCheckedItem(R.id.nav_home);
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(currentFragment instanceof HomeFragment)) {
                    loadFragment(new Loading()); // Load the loading fragment
                    new Handler().postDelayed(() -> {
                        loadFragment(new HomeFragment());
                        Share(myAppLink, shareMessage);
                    }, 2000);
                } else {
                    Share(myAppLink, shareMessage);
                }
                toolbar.setTitle("Heal");
            }

            // Update the previous selected item and view
            if (previousMenuItem != item) {
                previousMenuItem = item;
                previousItemView = currentItemView;
                if (previousItemView != null && id != R.id.nav_send && id != R.id.nav_share) {
                    previousItemView.setBackgroundColor(getResources().getColor(R.color.orange));
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        private void Share(String appLink, String optionalText) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, appLink + (optionalText != null && !optionalText.isEmpty() ? "\n\n" + optionalText : ""));
            shareIntent.setType("text/plain"); // Set the MIME type to plain text

            Intent chooserIntent = Intent.createChooser(shareIntent, "Share app link via");
            try {
                startActivity(chooserIntent);
            } catch (android.content.ActivityNotFoundException anfe) {
                Toast.makeText(this, "No app can handle this share action.", Toast.LENGTH_SHORT).show();
            }
        }

        // Example usage:



        private void showSendPopup() {
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            View popupView = inflater.inflate(R.layout.send_window, null);
            popupWindow = new PopupWindow(popupView, 900, ConstraintLayout.LayoutParams.WRAP_CONTENT, true);
            popupWindow.setFocusable(true);
            try {
                popupWindow.showAtLocation(fragmentMain, Gravity.CENTER, 0, 0);

                // Find the button and set the listener *after* the popup view is created
                Button sendMessageButtonPopup = popupView.findViewById(R.id.buttonSend);
                if (sendMessageButtonPopup != null) {
                    sendMessageButtonPopup.setOnClickListener(v -> {
                        EditText recipientEditTextPopup = popupView.findViewById(R.id.editTextRecipient);
                        EditText messageEditTextPopup = popupView.findViewById(R.id.editTextMessage);
                        if (recipientEditTextPopup != null && recipientEditTextPopup.length() == 0) {
                            Toast.makeText(this, "User ID Cannot Be empty", Toast.LENGTH_SHORT).show();
                        } else if (messageEditTextPopup != null && messageEditTextPopup.length() == 0) {
                            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
                        } else {
                            dismissSendPopupAndSaveData();
                        }
                    });
                } else {
                    Log.e(TAG, "buttonSend not found in send_window.xml");
                    Toast.makeText(this, "Error: Send button not found", Toast.LENGTH_SHORT).show();
                }

                Button cancelMessageButtonPopup = popupView.findViewById(R.id.buttonCancel);
                if (cancelMessageButtonPopup != null) {
                    cancelMessageButtonPopup.setOnClickListener(v -> {
                        if (popupWindow != null && popupWindow.isShowing()) {
                            popupWindow.dismiss();
                            popupWindow = null;
                        }
                    });
                } else {
                    Log.e(TAG, "buttonCancel not found in send_window.xml");
                    Toast.makeText(this, "Error: Cancel button not found", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Toast.makeText(this, "popup error", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error showing popup", e);
            }
        }


        private void dismissSendPopupAndSaveData() {
            if (popupWindow != null && popupWindow.isShowing()) {
                // Get the content view of the popup window
                View popupContentView = popupWindow.getContentView();

                // Find your input fields within the popupView and save their data
                EditText recipientEditText = popupContentView.findViewById(R.id.editTextRecipient); // Replace with your actual ID
                EditText messageEditText = popupContentView.findViewById(R.id.editTextMessage);   // Replace with your actual ID

                if (recipientEditText != null) {
                    sendData.put("recipient", recipientEditText.getText().toString());
                }
                if (messageEditText != null) {
                    sendData.put("message", messageEditText.getText().toString());
                }

                // Dismiss the popup window
                popupWindow.dismiss();
                popupWindow = null; // It's good practice to set it to null after dismissing

                // Now you can use the 'sendData' HashMap to access the saved input
                // For example:
                String savedRecipient = sendData.get("recipient");
                String savedMessage = sendData.get("message");
                // Do something with the saved data (e.g., store it, process it)
                Toast.makeText(this, "Message sent to " + savedRecipient, Toast.LENGTH_SHORT).show();
                Log.d("SendData", "Recipient: " + savedRecipient + ", Message: " + savedMessage);
            }
        }
        // Loads a fragment into the main content area
        private void loadFragment(Fragment fragment) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.fragment_container, fragment);
            ft.commit();
        }

        // Method to show/hide the bottom sheet
        public void toggleBottomSheet() {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                
                bottomSheetContent.setBackground(getResources().getDrawable(R.drawable.rounded_top_container));
            } else if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(STATE_COLLAPSED);
                bottomSheetContent.setBackgroundColor(getResources().getColor(R. color.transparent));
            }else{
                bottomSheetBehavior.setState(STATE_COLLAPSED);
                bottomSheetContent.setBackgroundColor(getResources().getColor(R. color.transparent));
            }
        }
        private void loadBottomFragment(Fragment fragment) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.bottom_sheet_content, fragment);
            ft.commit();
        }
        private void clearBottomFragment() {
            FragmentManager fm = getSupportFragmentManager();
            Fragment currentFragment = fm.findFragmentById(R.id.bottom_sheet_content);
            if (currentFragment != null) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.remove(currentFragment);
                ft.commit();
                fm.executePendingTransactions(); // Ensure the removal happens immediately
            }
        }
    }

