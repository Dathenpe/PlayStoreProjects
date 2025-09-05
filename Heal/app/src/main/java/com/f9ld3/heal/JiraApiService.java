package com.f9ld3.heal;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface JiraApiService {

    @POST("/rest/api/2/issue") // Or "/rest/api/3/issue" for newer Jira Cloud instances
    Call<JiraIssueResponse> createIssue(
            @Header("Authorization") String authHeader,
            @Body JiraIssueRequest issue
    );

    @POST("/rest/api/2/issue/{issueKeyOrId}/attachments") // Or "/rest/api/3/issue/{issueKeyOrId}/attachments"
    @Multipart
    Call<ResponseBody> addAttachment(
            @Header("Authorization") String authHeader,
            @Header("X-Atlassian-Token") String tokenHeader, // Usually "no-check"
            @Path("issueKeyOrId") String issueKeyOrId,
            @Part MultipartBody.Part file
    );
}
