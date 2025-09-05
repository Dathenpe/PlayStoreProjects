package com.f9ld3.heal;

import java.util.concurrent.TimeUnit;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.f9ld3.heal.BuildConfig;

public class RetrofitClient {

    // Using volatile for thread safety with double-checked locking
    private static volatile JiraApiService jiraApiServiceInstance;

    private static OkHttpClient getOkHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        // Control logging level based on BuildConfig.DEBUG
        loggingInterceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);

        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                 .connectTimeout(30, TimeUnit.SECONDS)
                 .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static JiraApiService getJiraApiService() {
        if (jiraApiServiceInstance == null) {
            synchronized (RetrofitClient.class) { // Synchronize for thread-safe initialization
                if (jiraApiServiceInstance == null) { // Double-check
                    if (BuildConfig.JIRA_BASE_URL == null || BuildConfig.JIRA_BASE_URL.isEmpty()) {
                        throw new IllegalStateException("JIRA_BASE_URL is not configured in BuildConfig. Check gradle.properties and build.gradle.");
                    }
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(BuildConfig.JIRA_BASE_URL) // From BuildConfig
                            .client(getOkHttpClient())
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    jiraApiServiceInstance = retrofit.create(JiraApiService.class);
                }
            }
        }
        return jiraApiServiceInstance;
    }

    public static String getAuthHeader() {
        if (BuildConfig.JIRA_API_USERNAME == null || BuildConfig.JIRA_API_USERNAME.isEmpty() ||
                BuildConfig.JIRA_API_TOKEN == null || BuildConfig.JIRA_API_TOKEN.isEmpty()) {
            // Or handle this more gracefully, perhaps by preventing the API call earlier
            throw new IllegalStateException("Jira API Username or Token is not configured in BuildConfig.");
        }
        return Credentials.basic(BuildConfig.JIRA_API_USERNAME, BuildConfig.JIRA_API_TOKEN);
    }
}
