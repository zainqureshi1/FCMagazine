package com.e2esp.fcmagazine.utils;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

/**
 * Created by Zain on 8/21/2017.
 */

public class DbClient {

    // LIVE
    private static final String ACCESS_TOKEN = "BQAJtDp5FOcAAAAAAAAL83j3c52JX_jR4GSVfPl2WOxLZ83Luu2Nhm1UUim7Qjqa";
    // TEST
    //private static final String ACCESS_TOKEN = "t3HP7BPiD2AAAAAAAAAAHzZCvsP_y-pkY1kv0PCAPSdxi13bKay5dwS0xQbRsWqE";

    private static DbxClientV2 dbClient;

    public static DbxClientV2 getDbClient() {
        if (dbClient == null) {
            DbxRequestConfig config = DbxRequestConfig.newBuilder("FC Magazine").build();
            dbClient = new DbxClientV2(config, ACCESS_TOKEN);
        }
        return dbClient;
    }

}
