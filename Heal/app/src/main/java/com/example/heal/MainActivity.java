package com.example.heal; // Replace this!

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import ui.SettingsFragment;
import ui.SlideshowFragment;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private MenuItem previousMenuItem;
    private View previousItemView;
    private ImageButton MenuTrigger;

    // Bottom Sheet related variables
    FrameLayout bottomSheetContent;
    View bottomSheetView;
    BottomSheetBehavior<View> bottomSheetBehavior;// Use the settings binding!

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
            loadFragment(new HomeFragment());
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
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); // Initial state

        // Inflate the settings layout into the bottom sheet


        // Now you can access views in your settings layout using settingsBinding
        // For example:
        // settingsBinding.settingTitleProfile.setText("My Settings");
        // settingsBinding.switchPush.setOnCheckedChangeListener(...);

        MenuTrigger = findViewById(R.id.menu_trigger);
        MenuTrigger.setOnClickListener(v -> {
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            View popupView = inflater.inflate(R.layout.custom_menu_layout, null);

            PopupWindow popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
            popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

            TextView settings = popupView.findViewById(R.id.menu_item_1);
            settings.setOnClickListener(v1 -> {

                loadBottomFragment(new SettingsFragment());
                popupWindow.dismiss();
            });

            TextView logout = popupView.findViewById(R.id.menu_item_2);
            logout.setOnClickListener(view -> {
                Toast.makeText(MainActivity.this, "Another Option Clicked", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
            });

            popupWindow.showAsDropDown(v);
        });
    }
    private void loadBottomFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.bottom_sheet_content, fragment);
        ft.commit();
    }

    private View findNavigationViewItemView(@NonNull MenuItem item) {
        if (navigationView == null) return null;
        return navigationView.findViewById(item.getItemId());
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        View currentItemView = navigationView.findViewById(id);

        if (previousItemView != null) {
            previousItemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }

        if (currentItemView != null) {
            currentItemView.setBackgroundResource(android.R.drawable.list_selector_background);
            currentItemView.setBackgroundColor(android.R.drawable.list_selector_background);
            Toast.makeText(this, "item selected" + id, Toast.LENGTH_SHORT).show();
        } else {
            Log.w("MainActivity", "Could not find View for MenuItem: " + item.getTitle() + " with ID: " + id);
        }

        if (id == R.id.nav_home) {
            loadFragment(new HomeFragment());
            toolbar.setTitle("Home Room");
        } else if (id == R.id.nav_gallery) {
            loadFragment(new GalleryFragment());
            toolbar.setTitle("Art Corner");
        } else if (id == R.id.nav_slideshow) {
            loadFragment(new SlideshowFragment());
            toolbar.setTitle("Game Room");
        }

        previousMenuItem = item;
        previousItemView = currentItemView;
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

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
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }
}