package com.connfa.service.firebase.model;

import java.util.List;

public class FirebasePoi {

    public Long poiId;

    public String poiName;

    public String poiDescription;

    public String poiImageURL;

    public String poiDetailURL;

    public Long order;

    public Boolean deleted;

    public static class Holder {

        public List<FirebasePoi> poi;

    }
}
