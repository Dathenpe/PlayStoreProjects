package com.f9ld3.xavier.ai.V2.utils;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A gazetteer for hardcoded locations to improve speed and resolve ambiguity.
 * This makes responses for common queries instant and reliable.
 */
public final class LocationCache {

private static final Map<String, JsonObject> CACHE = new HashMap<>();

// Static initializer to pre-load our known locations.
static {
	// --- North America ---
	// Countries & Major Cities
	addLocation("canada", "Ottawa", "CA", 45.4215, -75.6972);
	addLocation("usa", "Washington D.C.", "US", 38.9072, -77.0369);
	addLocation("united states", "Washington D.C.", "US", 38.9072, -77.0369);
	addLocation("united states of america", "Washington D.C.", "US", 38.9072, -77.0369);
	addLocation("mexico", "Mexico City", "MX", 19.4326, -99.1332);
	addLocation("guadalajara", "Guadalajara", "MX", 20.6597, -103.3496);
	addLocation("monterrey", "Monterrey", "MX", 25.6866, -100.3161);
	addLocation("tijuana", "Tijuana", "MX", 32.5149, -117.0382);
	addLocation("cancun", "Cancún", "MX", 21.1619, -86.8515);
	addLocation("greenland", "Nuuk", "GL", 64.1836, -51.7216);
	
	// Central America
	addLocation("guatemala", "Guatemala City", "GT", 14.6349, -90.5069);
	addLocation("belize", "Belmopan", "BZ", 17.2510, -88.7590);
	addLocation("el salvador", "San Salvador", "SV", 13.6929, -89.2182);
	addLocation("honduras", "Tegucigalpa", "HN", 14.0723, -87.1921);
	addLocation("nicaragua", "Managua", "NI", 12.1150, -86.2362);
	addLocation("costa rica", "San José", "CR", 9.9281, -84.0907);
	addLocation("panama", "Panama City", "PA", 8.9833, -79.5167);
	
	// Caribbean
	addLocation("cuba", "Havana", "CU", 23.1136, -82.3666);
	addLocation("jamaica", "Kingston", "JM", 17.9836, -76.8036);
	addLocation("haiti", "Port-au-Prince", "HT", 18.5944, -72.3074);
	addLocation("dominican republic", "Santo Domingo", "DO", 18.4861, -69.9312);
	addLocation("puerto rico", "San Juan", "PR", 18.4655, -66.1057);
	addLocation("the bahamas", "Nassau", "BS", 25.0479, -77.3554);
	addLocation("bahamas", "Nassau", "BS", 25.0479, -77.3554);
	addLocation("barbados", "Bridgetown", "BB", 13.1059, -59.6132);
	addLocation("trinidad and tobago", "Port of Spain", "TT", 10.6548, -61.5121);
	
	// US States & Major Cities
	addLocation("alabama", "Montgomery", "US", 32.3668, -86.3000);
	addLocation("alaska", "Juneau", "US", 58.3019, -134.4197);
	addLocation("arizona", "Phoenix", "US", 33.4484, -112.0740);
	addLocation("arkansas", "Little Rock", "US", 34.7465, -92.2896);
	addLocation("california", "Sacramento", "US", 38.5816, -121.4944);
	addLocation("los angeles", "Los Angeles", "US", 34.0522, -118.2437);
	addLocation("la", "Los Angeles", "US", 34.0522, -118.2437);
	addLocation("san diego", "San Diego", "US", 32.7157, -117.1611);
	addLocation("san francisco", "San Francisco", "US", 37.7749, -122.4194);
	addLocation("colorado", "Denver", "US", 39.7392, -104.9903);
	addLocation("connecticut", "Hartford", "US", 41.7658, -72.6734);
	addLocation("delaware", "Dover", "US", 39.1582, -75.5244);
	addLocation("florida", "Tallahassee", "US", 30.4383, -84.2807);
	addLocation("miami", "Miami", "US", 25.7617, -80.1918);
	addLocation("orlando", "Orlando", "US", 28.5383, -81.3792);
	addLocation("georgia", "Atlanta", "US", 33.7490, -84.3880);
	addLocation("hawaii", "Honolulu", "US", 21.3069, -157.8583);
	addLocation("idaho", "Boise", "US", 43.6150, -116.2023);
	addLocation("illinois", "Springfield", "US", 39.7817, -89.6501);
	addLocation("chicago", "Chicago", "US", 41.8781, -87.6298);
	addLocation("indiana", "Indianapolis", "US", 39.7684, -86.1581);
	addLocation("iowa", "Des Moines", "US", 41.5868, -93.6250);
	addLocation("kansas", "Topeka", "US", 39.0473, -95.6752);
	addLocation("kentucky", "Frankfort", "US", 38.2009, -84.8733);
	addLocation("louisiana", "Baton Rouge", "US", 30.4515, -91.1871);
	addLocation("new orleans", "New Orleans", "US", 29.9511, -90.0715);
	addLocation("maine", "Augusta", "US", 44.3106, -69.7795);
	addLocation("maryland", "Annapolis", "US", 38.9784, -76.4922);
	addLocation("baltimore", "Baltimore", "US", 39.2904, -76.6122);
	addLocation("massachusetts", "Boston", "US", 42.3601, -71.0589);
	addLocation("michigan", "Lansing", "US", 42.7325, -84.5555);
	addLocation("detroit", "Detroit", "US", 42.3314, -83.0458);
	addLocation("minnesota", "Saint Paul", "US", 44.9537, -93.0900);
	addLocation("minneapolis", "Minneapolis", "US", 44.9778, -93.2650);
	addLocation("mississippi", "Jackson", "US", 32.2988, -90.1848);
	addLocation("missouri", "Jefferson City", "US", 38.5767, -92.1735);
	addLocation("st. louis", "St. Louis", "US", 38.6270, -90.1994);
	addLocation("montana", "Helena", "US", 46.5891, -112.0391);
	addLocation("nebraska", "Lincoln", "US", 40.8136, -96.7026);
	addLocation("nevada", "Carson City", "US", 39.1638, -119.7674);
	addLocation("las vegas", "Las Vegas", "US", 36.1699, -115.1398);
	addLocation("new hampshire", "Concord", "US", 43.2081, -71.5376);
	addLocation("new jersey", "Trenton", "US", 40.2171, -74.7429);
	addLocation("new mexico", "Santa Fe", "US", 35.6870, -105.9378);
	addLocation("new york", "Albany", "US", 42.6526, -73.7562);
	addLocation("new york city", "New York City", "US", 40.7128, -74.0060);
	addLocation("nyc", "New York City", "US", 40.7128, -74.0060);
	addLocation("north carolina", "Raleigh", "US", 35.7796, -78.6382);
	addLocation("charlotte", "Charlotte", "US", 35.2271, -80.8431);
	addLocation("north dakota", "Bismarck", "US", 46.8083, -100.7837);
	addLocation("ohio", "Columbus", "US", 39.9612, -82.9988);
	addLocation("cleveland", "Cleveland", "US", 41.4993, -81.6944);
	addLocation("oklahoma", "Oklahoma City", "US", 35.4676, -97.5164);
	addLocation("oregon", "Salem", "US", 44.9429, -123.0351);
	addLocation("portland", "Portland", "US", 45.5051, -122.6750);
	addLocation("pennsylvania", "Harrisburg", "US", 40.2732, -76.8867);
	addLocation("philadelphia", "Philadelphia", "US", 39.9526, -75.1652);
	addLocation("rhode island", "Providence", "US", 41.8240, -71.4128);
	addLocation("south carolina", "Columbia", "US", 34.0007, -81.0348);
	addLocation("south dakota", "Pierre", "US", 44.3683, -100.3510);
	addLocation("tennessee", "Nashville", "US", 36.1627, -86.7816);
	addLocation("memphis", "Memphis", "US", 35.1495, -90.0490);
	addLocation("texas", "Austin", "US", 30.2672, -97.7431);
	addLocation("houston", "Houston", "US", 29.7604, -95.3698);
	addLocation("dallas", "Dallas", "US", 32.7767, -96.7970);
	addLocation("san antonio", "San Antonio", "US", 29.4241, -98.4936);
	addLocation("utah", "Salt Lake City", "US", 40.7608, -111.8910);
	addLocation("vermont", "Montpelier", "US", 44.2601, -72.5754);
	addLocation("virginia", "Richmond", "US", 37.5407, -77.4360);
	addLocation("washington", "Olympia", "US", 47.0379, -122.9007);
	addLocation("seattle", "Seattle", "US", 47.6062, -122.3321);
	addLocation("west virginia", "Charleston", "US", 38.3498, -81.6326);
	addLocation("wisconsin", "Madison", "US", 43.0731, -89.4012);
	addLocation("milwaukee", "Milwaukee", "US", 43.0389, -87.9065);
	addLocation("wyoming", "Cheyenne", "US", 41.1400, -104.8202);
	
	// Canadian Provinces, Territories, & Major Cities
	addLocation("alberta", "Edmonton", "CA", 53.5461, -113.4938);
	addLocation("calgary", "Calgary", "CA", 51.0447, -114.0719);
	addLocation("british columbia", "Victoria", "CA", 48.4284, -123.3656);
	addLocation("vancouver", "Vancouver", "CA", 49.2827, -123.1207);
	addLocation("manitoba", "Winnipeg", "CA", 49.8951, -97.1384);
	addLocation("new brunswick", "Fredericton", "CA", 45.9636, -66.6431);
	addLocation("newfoundland and labrador", "St. John's", "CA", 47.5615, -52.7126);
	addLocation("nova scotia", "Halifax", "CA", 44.6488, -63.5752);
	addLocation("ontario", "Toronto", "CA", 43.6532, -79.3832);
	addLocation("toronto", "Toronto", "CA", 43.6532, -79.3832);
	addLocation("prince edward island", "Charlottetown", "CA", 46.2382, -63.1311);
	addLocation("quebec", "Quebec City", "CA", 46.8139, -71.2080);
	addLocation("montreal", "Montreal", "CA", 45.5017, -73.5673);
	addLocation("saskatchewan", "Regina", "CA", 50.4452, -104.6189);
	addLocation("northwest territories", "Yellowknife", "CA", 62.4540, -114.3718);
	addLocation("nunavut", "Iqaluit", "CA", 63.7467, -68.5170);
	addLocation("yukon", "Whitehorse", "CA", 60.7212, -135.0568);
	
	// --- Europe ---
	addLocation("uk", "London", "GB", 51.5072, -0.1276);
	addLocation("united kingdom", "London", "GB", 51.5072, -0.1276);
	addLocation("england", "London", "GB", 51.5072, -0.1276);
	addLocation("manchester", "Manchester", "GB", 53.4808, -2.2426);
	addLocation("birmingham", "Birmingham", "GB", 52.4862, -1.8904);
	addLocation("liverpool", "Liverpool", "GB", 53.4084, -2.9916);
	addLocation("scotland", "Edinburgh", "GB", 55.9533, -3.1883);
	addLocation("glasgow", "Glasgow", "GB", 55.8642, -4.2518);
	addLocation("wales", "Cardiff", "GB", 51.4816, -3.1791);
	addLocation("northern ireland", "Belfast", "GB", 54.5973, -5.9301);
	addLocation("ireland", "Dublin", "IE", 53.3498, -6.2603);
	addLocation("germany", "Berlin", "DE", 52.5200, 13.4050);
	addLocation("hamburg", "Hamburg", "DE", 53.5511, 9.9937);
	addLocation("munich", "Munich", "DE", 48.1351, 11.5820);
	addLocation("cologne", "Cologne", "DE", 50.9375, 6.9603);
	addLocation("frankfurt", "Frankfurt", "DE", 50.1109, 8.6821);
	addLocation("france", "Paris", "FR", 48.8566, 2.3522);
	addLocation("marseille", "Marseille", "FR", 43.2965, 5.3698);
	addLocation("lyon", "Lyon", "FR", 45.7640, 4.8357);
	addLocation("nice", "Nice", "FR", 43.7102, 7.2620);
	addLocation("italy", "Rome", "IT", 41.9028, 12.4964);
	addLocation("milan", "Milan", "IT", 45.4642, 9.1900);
	addLocation("naples", "Naples", "IT", 40.8518, 14.2681);
	addLocation("turin", "Turin", "IT", 45.0703, 7.6869);
	addLocation("florence", "Florence", "IT", 43.7696, 11.2558);
	addLocation("venice", "Venice", "IT", 45.4408, 12.3155);
	addLocation("spain", "Madrid", "ES", 40.4168, -3.7038);
	addLocation("barcelona", "Barcelona", "ES", 41.3851, 2.1734);
	addLocation("valencia", "Valencia", "ES", 39.4699, -0.3763);
	addLocation("seville", "Seville", "ES", 37.3891, -5.9845);
	addLocation("portugal", "Lisbon", "PT", 38.7223, -9.1393);
	addLocation("netherlands", "Amsterdam", "NL", 52.3676, 4.9041);
	addLocation("rotterdam", "Rotterdam", "NL", 51.9244, 4.4777);
	addLocation("the hague", "The Hague", "NL", 52.0787, 4.2886);
	addLocation("belgium", "Brussels", "BE", 50.8503, 4.3517);
	addLocation("luxembourg", "Luxembourg", "LU", 49.6116, 6.1319);
	addLocation("switzerland", "Bern", "CH", 46.9480, 7.4474);
	addLocation("zurich", "Zurich", "CH", 47.3769, 8.5417);
	addLocation("geneva", "Geneva", "CH", 46.2044, 6.1432);
	addLocation("austria", "Vienna", "AT", 48.2082, 16.3738);
	addLocation("poland", "Warsaw", "PL", 52.2297, 21.0122);
	addLocation("krakow", "Kraków", "PL", 50.0647, 19.9450);
	addLocation("greece", "Athens", "GR", 37.9838, 23.7275);
	addLocation("sweden", "Stockholm", "SE", 59.3293, 18.0686);
	addLocation("norway", "Oslo", "NO", 59.9139, 10.7522);
	addLocation("denmark", "Copenhagen", "DK", 55.6761, 12.5683);
	addLocation("finland", "Helsinki", "FI", 60.1699, 24.9384);
	addLocation("iceland", "Reykjavik", "IS", 64.1466, -21.9426);
	addLocation("russia", "Moscow", "RU", 55.7558, 37.6173);
	addLocation("saint petersburg", "Saint Petersburg", "RU", 59.9311, 30.3609);
	addLocation("ukraine", "Kyiv", "UA", 50.4501, 30.5234);
	addLocation("czech republic", "Prague", "CZ", 50.0755, 14.4378);
	addLocation("hungary", "Budapest", "HU", 47.4979, 19.0402);
	addLocation("romania", "Bucharest", "RO", 44.4268, 26.1025);
	addLocation("bulgaria", "Sofia", "BG", 42.6977, 23.3219);
	addLocation("croatia", "Zagreb", "HR", 45.8150, 15.9819);
	addLocation("serbia", "Belgrade", "RS", 44.7866, 20.4489);
	addLocation("slovenia", "Ljubljana", "SI", 46.0569, 14.5058);
	addLocation("slovakia", "Bratislava", "SK", 48.1486, 17.1077);
	addLocation("belarus", "Minsk", "BY", 53.9045, 27.5615);
	addLocation("lithuania", "Vilnius", "LT", 54.6872, 25.2797);
	addLocation("latvia", "Riga", "LV", 56.9496, 24.1052);
	addLocation("estonia", "Tallinn", "EE", 59.4370, 24.7536);
	addLocation("monaco", "Monaco", "MC", 43.7384, 7.4246);
	addLocation("vatican city", "Vatican City", "VA", 41.9029, 12.4534);
	addLocation("malta", "Valletta", "MT", 35.8989, 14.5146);
	addLocation("cyprus", "Nicosia", "CY", 35.1856, 33.3823);
	
	// --- Asia ---
	addLocation("china", "Beijing", "CN", 39.9042, 116.4074);
	addLocation("shanghai", "Shanghai", "CN", 31.2304, 121.4737);
	addLocation("chongqing", "Chongqing", "CN", 29.5630, 106.5516);
	addLocation("tianjin", "Tianjin", "CN", 39.0842, 117.2009);
	addLocation("guangzhou", "Guangzhou", "CN", 23.1291, 113.2644);
	addLocation("shenzhen", "Shenzhen", "CN", 22.5431, 114.0579);
	addLocation("hong kong", "Hong Kong", "HK", 22.3193, 114.1694);
	addLocation("macau", "Macau", "MO", 22.1987, 113.5439);
	addLocation("japan", "Tokyo", "JP", 35.6762, 139.6503);
	addLocation("osaka", "Osaka", "JP", 34.6937, 135.5023);
	addLocation("kyoto", "Kyoto", "JP", 35.0116, 135.7681);
	addLocation("india", "New Delhi", "IN", 28.6139, 77.2090);
	addLocation("mumbai", "Mumbai", "IN", 19.0760, 72.8777);
	addLocation("bangalore", "Bangalore", "IN", 12.9716, 77.5946);
	addLocation("kolkata", "Kolkata", "IN", 22.5726, 88.3639);
	addLocation("chennai", "Chennai", "IN", 13.0827, 80.2707);
	addLocation("south korea", "Seoul", "KR", 37.5665, 126.9780);
	addLocation("busan", "Busan", "KR", 35.1796, 129.0756);
	addLocation("north korea", "Pyongyang", "KP", 39.0392, 125.7625);
	addLocation("taiwan", "Taipei", "TW", 25.0330, 121.5654);
	addLocation("indonesia", "Jakarta", "ID", -6.2088, 106.8456);
	addLocation("thailand", "Bangkok", "TH", 13.7563, 100.5018);
	addLocation("vietnam", "Hanoi", "VN", 21.0278, 105.8342);
	addLocation("ho chi minh city", "Ho Chi Minh City", "VN", 10.7769, 106.7009);
	addLocation("malaysia", "Kuala Lumpur", "MY", 3.1390, 101.6869);
	addLocation("singapore", "Singapore", "SG", 1.3521, 103.8198);
	addLocation("philippines", "Manila", "PH", 14.5995, 120.9842);
	addLocation("pakistan", "Islamabad", "PK", 33.6844, 73.0479);
	addLocation("karachi", "Karachi", "PK", 24.8607, 67.0011);
	addLocation("bangladesh", "Dhaka", "BD", 23.8103, 90.4125);
	addLocation("nepal", "Kathmandu", "NP", 27.7172, 85.3240);
	addLocation("sri lanka", "Sri Jayawardenepura Kotte", "LK", 6.8943, 79.9234);
	addLocation("colombo", "Colombo", "LK", 6.9271, 79.8612);
	addLocation("afghanistan", "Kabul", "AF", 34.5553, 69.2075);
	addLocation("iran", "Tehran", "IR", 35.6892, 51.3890);
	addLocation("iraq", "Baghdad", "IQ", 33.3152, 44.3661);
	addLocation("saudi arabia", "Riyadh", "SA", 24.7136, 46.6753);
	addLocation("uae", "Abu Dhabi", "AE", 24.4539, 54.3773);
	addLocation("united arab emirates", "Abu Dhabi", "AE", 24.4539, 54.3773);
	addLocation("dubai", "Dubai", "AE", 25.2048, 55.2708);
	addLocation("turkey", "Ankara", "TR", 39.9334, 32.8600);
	addLocation("istanbul", "Istanbul", "TR", 41.0082, 28.9784);
	addLocation("israel", "Jerusalem", "IL", 31.7683, 35.2137);
	addLocation("tel aviv", "Tel Aviv", "IL", 32.0853, 34.7818);
	addLocation("jordan", "Amman", "JO", 31.9454, 35.9284);
	addLocation("lebanon", "Beirut", "LB", 33.8938, 35.5018);
	addLocation("syria", "Damascus", "SY", 33.5138, 36.2765);
	addLocation("qatar", "Doha", "QA", 25.2854, 51.5310);
	addLocation("kuwait", "Kuwait City", "KW", 29.3759, 47.9774);
	addLocation("bahrain", "Manama", "BH", 26.2285, 50.5860);
	addLocation("oman", "Muscat", "OM", 23.5859, 58.3839);
	addLocation("yemen", "Sana'a", "YE", 15.3694, 44.1910);
	addLocation("kazakhstan", "Astana", "KZ", 51.1694, 71.4491);
	addLocation("uzbekistan", "Tashkent", "UZ", 41.2995, 69.2401);
	addLocation("turkmenistan", "Ashgabat", "TM", 37.9601, 58.3261);
	addLocation("kyrgyzstan", "Bishkek", "KG", 42.8746, 74.5698);
	addLocation("tajikistan", "Dushanbe", "TJ", 38.5598, 68.7870);
	addLocation("mongolia", "Ulaanbaatar", "MN", 47.9185, 106.9177);
	addLocation("cambodia", "Phnom Penh", "KH", 11.5564, 104.9282);
	addLocation("myanmar", "Naypyidaw", "MM", 19.7633, 96.0785);
	addLocation("laos", "Vientiane", "LA", 17.9748, 102.6309);
	addLocation("brunei", "Bandar Seri Begawan", "BN", 4.9031, 114.9398);
	
	// --- South America (Comprehensive List) ---
	addLocation("brazil", "Brasília", "BR", -15.8267, -47.9218);
	addLocation("sao paulo", "São Paulo", "BR", -23.5505, -46.6333);
	addLocation("rio de janeiro", "Rio de Janeiro", "BR", -22.9068, -43.1729);
	addLocation("argentina", "Buenos Aires", "AR", -34.6037, -58.3816);
	addLocation("colombia", "Bogotá", "CO", 4.7110, -74.0721);
	addLocation("medellin", "Medellín", "CO", 6.2442, -75.5812);
	addLocation("peru", "Lima", "PE", -12.0464, -77.0428);
	addLocation("chile", "Santiago", "CL", -33.4489, -70.6693);
	addLocation("ecuador", "Quito", "EC", -0.1807, -78.4678);
	addLocation("guayaquil", "Guayaquil", "EC", -2.1710, -79.9224);
	addLocation("venezuela", "Caracas", "VE", 10.4806, -66.9036);
	addLocation("bolivia", "Sucre", "BO", -19.0196, -65.2619);
	addLocation("la paz", "La Paz", "BO", -16.4897, -68.1193); // Seat of government
	addLocation("paraguay", "Asunción", "PY", -25.2637, -57.5759);
	addLocation("uruguay", "Montevideo", "UY", -34.9011, -56.1645);
	addLocation("guyana", "Georgetown", "GY", 6.8013, -58.1551);
	addLocation("suriname", "Paramaribo", "SR", 5.8520, -55.2038);
	addLocation("french guiana", "Cayenne", "GF", 4.9372, -52.3260);
	
	// --- Africa (Greatly Expanded) ---
	addLocation("nigeria", "Abuja", "NG", 9.0765, 7.3986);
	addLocation("lagos", "Lagos", "NG", 6.5244, 3.3792);
	addLocation("kano", "Kano", "NG", 12.0022, 8.5920);
	addLocation("ibadan", "Ibadan", "NG", 7.3776, 3.9470);
	addLocation("egypt", "Cairo", "EG", 30.0444, 31.2357);
	addLocation("alexandria", "Alexandria", "EG", 31.2001, 29.9187);
	addLocation("south africa", "Pretoria", "ZA", -25.7479, 28.2293);
	addLocation("cape town", "Cape Town", "ZA", -33.9249, 18.4241);
	addLocation("johannesburg", "Johannesburg", "ZA", -26.2041, 28.0473);
	addLocation("durban", "Durban", "ZA", -29.8587, 31.0218);
	addLocation("kenya", "Nairobi", "KE", -1.2921, 36.8219);
	addLocation("ghana", "Accra", "GH", 5.6037, -0.1870);
	addLocation("ethiopia", "Addis Ababa", "ET", 9.0194, 38.7525);
	addLocation("morocco", "Rabat", "MA", 34.0209, -6.8417);
	addLocation("casablanca", "Casablanca", "MA", 33.5731, -7.5898);
	addLocation("algeria", "Algiers", "DZ", 36.7754, 3.0601);
	addLocation("tanzania", "Dodoma", "TZ", -6.1630, 35.7516);
	addLocation("dar es salaam", "Dar es Salaam", "TZ", -6.7924, 39.2083);
	addLocation("sudan", "Khartoum", "SD", 15.5007, 32.5599);
	addLocation("uganda", "Kampala", "UG", 0.3476, 32.5825);
	addLocation("angola", "Luanda", "AO", -8.8399, 13.2894);
	addLocation("mozambique", "Maputo", "MZ", -25.9692, 32.5732);
	addLocation("madagascar", "Antananarivo", "MG", -18.8792, 47.5079);
	addLocation("cameroon", "Yaoundé", "CM", 3.8480, 11.5021);
	addLocation("ivory coast", "Yamoussoukro", "CI", 6.8205, -5.2767);
	addLocation("cote d'ivoire", "Yamoussoukro", "CI", 6.8205, -5.2767);
	addLocation("abidjan", "Abidjan", "CI", 5.3599, -4.0083);
	addLocation("burkina faso", "Ouagadougou", "BF", 12.3714, -1.5197);
	addLocation("niger", "Niamey", "NE", 13.5116, 2.1254);
	addLocation("mali", "Bamako", "ML", 12.6392, -8.0029);
	addLocation("senegal", "Dakar", "SN", 14.7167, -17.4677);
	addLocation("zimbabwe", "Harare", "ZW", -17.8252, 31.0335);
	addLocation("zambia", "Lusaka", "ZM", -15.3875, 28.3228);
	addLocation("rwanda", "Kigali", "RW", -1.9441, 30.0619);
	addLocation("tunisia", "Tunis", "TN", 36.8065, 10.1815);
	addLocation("guinea", "Conakry", "GN", 9.5379, -13.6773);
	addLocation("benin", "Porto-Novo", "BJ", 6.4969, 2.6293);
	addLocation("togo", "Lomé", "TG", 6.1319, 1.2228);
	addLocation("sierra leone", "Freetown", "SL", 8.4844, -13.2299);
	addLocation("liberia", "Monrovia", "LR", 6.3004, -10.7969);
	addLocation("libya", "Tripoli", "LY", 32.8872, 13.1913);
	addLocation("mauritania", "Nouakchott", "MR", 18.0735, -15.9582);
	addLocation("eritrea", "Asmara", "ER", 15.3229, 38.9251);
	addLocation("gambia", "Banjul", "GM", 13.4549, -16.5790);
	addLocation("botswana", "Gaborone", "BW", -24.6282, 25.9231);
	addLocation("namibia", "Windhoek", "NA", -22.5594, 17.0832);
	addLocation("gabon", "Libreville", "GA", 0.4162, 9.4673);
	addLocation("lesotho", "Maseru", "LS", -29.3159, 27.4870);
	addLocation("guinea-bissau", "Bissau", "GW", 11.8636, -15.5977);
	addLocation("equatorial guinea", "Malabo", "GQ", 3.7523, 8.7749);
	addLocation("djibouti", "Djibouti", "DJ", 11.5890, 43.1450);
	addLocation("comoros", "Moroni", "KM", -11.7022, 43.2551);
	addLocation("cape verde", "Praia", "CV", 14.9330, -23.5133);
	addLocation("sao tome and principe", "São Tomé", "ST", 0.3365, 6.7273);
	addLocation("seychelles", "Victoria", "SC", -4.6191, 55.4513);
	addLocation("mauritius", "Port Louis", "MU", -20.1609, 57.5012);
	
	// --- Oceania (Comprehensive List) ---
	addLocation("australia", "Canberra", "AU", -35.2809, 149.1300);
	addLocation("sydney", "Sydney", "AU", -33.8688, 151.2093);
	addLocation("melbourne", "Melbourne", "AU", -37.8136, 144.9631);
	addLocation("queensland", "Brisbane", "AU", -27.4698, 153.0251);
	addLocation("western australia", "Perth", "AU", -31.9505, 115.8605);
	addLocation("south australia", "Adelaide", "AU", -34.9285, 138.6007);
	addLocation("tasmania", "Hobart", "AU", -42.8821, 147.3272);
	addLocation("new zealand", "Wellington", "NZ", -41.2865, 174.7762);
	addLocation("auckland", "Auckland", "NZ", -36.8485, 174.7633);
	addLocation("christchurch", "Christchurch", "NZ", -43.5321, 172.6362);
	addLocation("fiji", "Suva", "FJ", -18.1416, 178.4419);
	addLocation("papua new guinea", "Port Moresby", "PG", -9.4438, 147.1803);
	addLocation("solomon islands", "Honiara", "SB", -9.4326, 159.9550);
	addLocation("vanuatu", "Port Vila", "VU", -17.7335, 168.3278);
	addLocation("samoa", "Apia", "WS", -13.8333, -171.7667);
	addLocation("tonga", "Nuku'alofa", "TO", -21.1393, -175.2023);
	addLocation("kiribati", "Tarawa", "KI", 1.4820, 173.0232);
	addLocation("micronesia", "Palikir", "FM", 6.9177, 158.1852);
	addLocation("marshall islands", "Majuro", "MH", 7.1105, 171.1834);
	addLocation("palau", "Ngerulmud", "PW", 7.5004, 134.6242);
	addLocation("tuvalu", "Funafuti", "TV", -8.5247, 179.1942);
	addLocation("nauru", "Yaren", "NR", -0.5477, 166.9209);
}
/**
 * A private helper to build a location JsonObject and add it to the cache.
 * This is more maintainable and less error-prone than parsing JSON strings.
 */
private static void addLocation(String key, String name, String country, double lat, double lon) {
	JsonObject locationData = new JsonObject();
	locationData.addProperty("name", name);
	locationData.addProperty("country", country);
	locationData.addProperty("lat", lat);
	locationData.addProperty("lon", lon);
	CACHE.put(key.toLowerCase(), locationData);
}

/**
 * Checks the cache for a known location.
 * @param location The location name to check.
 * @return An Optional containing the geocoding data if found, otherwise empty.
 */
public static Optional<JsonObject> get(String location) {
	return Optional.ofNullable(CACHE.get(location.toLowerCase().trim()));
}
public static Set<String> getAllKeys() {
	return CACHE.keySet();
}
}