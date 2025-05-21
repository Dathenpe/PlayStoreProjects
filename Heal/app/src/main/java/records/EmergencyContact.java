package records;

import java.io.Serializable;

public class EmergencyContact implements Serializable {
    private String id;
    private String name;
    private String phoneNumber;
    private String imageUrl; // New field for image URI

    public EmergencyContact(String id, String name, String phoneNumber, String imageUrl) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.imageUrl = imageUrl;
    }

    // Constructor for cases where image URL might not be provided initially
    public EmergencyContact(String id, String name, String phoneNumber) {
        this(id, name, phoneNumber, null); // Calls the main constructor with null imageUrl
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getImageUrl() { return imageUrl; }

    // Setters (for editing)
    public void setName(String name) { this.name = name; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}