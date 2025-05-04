package com.example.heal; // Replace this!

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import ui.HomeFragment;
import ui.GalleryFragment;
import ui.ItemListDialogFragment;
import ui.SlideshowFragment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.heal.R;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;

    private ImageButton MenuTrigger;
    private NavigationView navigationView;

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
        }
        MenuTrigger = findViewById(R.id.menu_trigger); // This will now find the button in the included toolbar
        MenuTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.custom_menu_layout, null);

                PopupWindow popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);

                // Set background to allow dismissal on outside touch
                popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                // Find menu items and set click listeners
                TextView menuItem1 = popupView.findViewById(R.id.menu_item_1);
                menuItem1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showItemListDialog(); // Call the method to show the DialogFragment
                        popupWindow.dismiss();
                    }
                });

                TextView menuItem2 = popupView.findViewById(R.id.menu_item_2);
                menuItem2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(MainActivity.this, "Another Option Clicked", Toast.LENGTH_SHORT).show();
                        popupWindow.dismiss();
                    }
                });

                // Show the popup window below the trigger view
                popupWindow.showAsDropDown(v);
            }
        });
    }

    private void showItemListDialog() {
        // Get the FragmentManager from the Activity
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Create an instance of your DialogFragment
        ItemListDialogFragment dialog = ItemListDialogFragment.newInstance(R.layout.fragment_item_list_dialog_list_dialog_item); // Pass any arguments you need (e.g., item count)

        // Show the DialogFragment
        dialog.show(fragmentManager, "item_list_dialog"); // Use a tag to identify the dialog
    }
    // What happens when you press the back button
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // This is where the magic happens when you click on a menu item!
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

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

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // This handy function helps us switch between "rooms" (Fragments)
    private void loadFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
    }
}