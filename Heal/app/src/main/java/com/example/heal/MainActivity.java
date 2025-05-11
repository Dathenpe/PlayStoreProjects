    package com.example.heal; // Replace this!


    import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;

    import android.graphics.Color;
    import android.graphics.drawable.ColorDrawable;
    import android.os.Bundle;
    import android.os.Handler;
    import android.util.Log;
    import android.view.Gravity;
    import android.view.LayoutInflater;
    import android.view.MenuItem;
    import android.view.View;
    import android.view.ViewGroup;
    import android.view.ViewTreeObserver;
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
    // Import the correct binding!

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
                previousMenuItem = navigationView.getMenu().findItem(R.id.nav_home);
                previousItemView = findNavigationViewItemView(previousMenuItem);
                if (previousItemView != null) {
                    previousItemView.setBackgroundColor(getResources().getColor(R.color.orange));
                }
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

            if (previousItemView != null) {
                previousItemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }

            if (id == R.id.nav_home) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(currentFragment instanceof HomeFragment)){
                    loadFragment(new Loading()); // Load the loading fragment
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            loadFragment(new HomeFragment()); // Load the HomeFragment after a delay
                        }
                    }, 2000);
                }
                navigationView.setCheckedItem(R.id.nav_home);
                toolbar.setTitle("Heal");
            } else if (id == R.id.nav_gallery) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(currentFragment instanceof GalleryFragment)){
                    loadFragment(new Loading()); // Load the loading fragment
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            loadFragment(new GalleryFragment()); // Load the HomeFragment after a delay
                        }
                    }, 2000);
                }
                navigationView.setCheckedItem(R.id.nav_gallery);
                toolbar.setTitle("Art Corner");
            } else if (id == R.id.nav_slideshow) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
               if (!(currentFragment instanceof SlideshowFragment)){
                   loadFragment(new Loading()); // Load the loading fragment
                   new Handler().postDelayed(new Runnable() {
                       @Override
                       public void run() {
                           loadFragment(new SlideshowFragment()); // Load the HomeFragment after a delay
                       }
                   }, 2000);
               }
                navigationView.setCheckedItem(R.id.nav_slideshow);
                toolbar.setTitle("Game Room");
            }else if (id == R.id.nav_send) {
                navigationView.setCheckedItem(R.id.nav_home);
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(currentFragment instanceof HomeFragment)) {
                    loadFragment(new Loading()); // Load the loading fragment
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            loadFragment(new HomeFragment());
                            showSendPopup();// Load the HomeFragment after a delay
                        }
                    }, 2000);
                } else {
                    // Optionally, add code here if the current fragment is already HomeFragment
                    showSendPopup(); // You might still want to show the popup
                }
                toolbar.setTitle("Heal");

            }else if (id == R.id.nav_share) {
                navigationView.setCheckedItem(R.id.nav_home);
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(currentFragment instanceof HomeFragment)) {
                    loadFragment(new Loading()); // Load the loading fragment
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            loadFragment(new HomeFragment());
                            showSendPopup();// Load the HomeFragment after a delay
                        }
                    }, 2000);
                } else {
                    // Optionally, add code here if the current fragment is already HomeFragment
                    showSendPopup(); // You might still want to show the popup
                }
                toolbar.setTitle("Heal");
            }

            previousMenuItem = item;
            previousItemView = currentItemView;
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        private void showSendPopup() {
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            View popupView = inflater.inflate(R.layout.send_window, null);
            popupWindow = new PopupWindow(popupView, ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT, true);
            popupWindow.setFocusable(true);
          try {
              popupWindow.showAtLocation(fragmentMain, Gravity.CENTER, 0, 0);
          }catch(Exception e){
              Toast.makeText(this, "popup error", Toast.LENGTH_SHORT).show();
          }
           // popupWindow.showAtLocation(fragmentMain, Gravity.CENTER, 0, 0);
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

