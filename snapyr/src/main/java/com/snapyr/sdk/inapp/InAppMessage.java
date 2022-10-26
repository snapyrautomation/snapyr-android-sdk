/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.snapyr.sdk.inapp;

import android.os.Parcel;
import android.os.Parcelable;

import com.snapyr.sdk.ValueMap;
import com.snapyr.sdk.internal.SnapyrAction;
import com.snapyr.sdk.internal.Utils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InAppMessage implements Parcelable {
    public static final SimpleDateFormat Formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public final Date Timestamp;
    public final InAppActionType ActionType;
    public final String ActionToken;
    public final InAppContent Content;
    public final String UserId;

    public InAppMessage(SnapyrAction action) throws MalformedMessageException {
        String rawTs = action.getString("timestamp");
        if (rawTs == null) {
            throw new MalformedMessageException("no timestamp in action");
        }
        try {
            this.Timestamp = Formatter.parse(rawTs);
        } catch (ParseException e) {
            throw new MalformedMessageException("malformed timestamp");
        }

        String actionType = action.getString("actionType");
        if (Utils.isNullOrEmpty(actionType)) {
            throw new MalformedMessageException("missing actionType in action");
        }
        this.UserId = action.getString("userId");

        switch (actionType) {
            case "custom":
                this.ActionType = InAppActionType.ACTION_TYPE_CUSTOM;
                break;
            case "overlay":
                this.ActionType = InAppActionType.ACTION_TYPE_OVERLAY;
                break;
            default:
                throw new MalformedMessageException("unknown content type: " + actionType);
        }

        this.ActionToken = action.getString("actionToken");
        if (Utils.isNullOrEmpty(this.ActionToken)) {
            throw new MalformedMessageException("missing actionToken in action");
        }
        this.Content = new InAppContent(action.getValueMap("content"));
    }

    public ValueMap asValueMap() {
        ValueMap map =
                new ValueMap()
                        .putValue("timestamp", Formatter.format(this.Timestamp))
                        .putValue("userId", this.UserId)
                        .putValue("actionToken", this.ActionToken)
                        .putValue(
                                "actionType",
                                (this.ActionType == InAppActionType.ACTION_TYPE_CUSTOM
                                        ? "custom"
                                        : "overlay"))
                        .putValue("content", this.Content.asValueMap());
        return map;
    }

    public static class MalformedMessageException extends Exception {
        public MalformedMessageException(String error) {
            super(error);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.asValueMap());


//        dest.writeLong(this.Timestamp != null ? this.Timestamp.getTime() : -1);
//        dest.writeInt(this.ActionType == null ? -1 : this.ActionType.ordinal());
//        dest.writeString(this.ActionToken);
//        dest.writeParcelable(this.Content, flags);
//        dest.writeString(this.UserId);
    }

//    public void readFromParcel(Parcel source) {
//
////        long tmpTimestamp = source.readLong();
////        this.Timestamp = tmpTimestamp == -1 ? null : new Date(tmpTimestamp);
////        int tmpActionType = source.readInt();
////        this.ActionType = tmpActionType == -1 ? null : InAppActionType.values()[tmpActionType];
////        this.ActionToken = source.readString();
////        this.Content = source.readParcelable(InAppContent.class.getClassLoader());
////        this.UserId = source.readString();
//    }



    public static final Parcelable.Creator<InAppMessage> CREATOR = new Parcelable.Creator<InAppMessage>() {
        @Override
        public InAppMessage createFromParcel(Parcel source) {
            SnapyrAction decodedAction = SnapyrAction.create( (ValueMap) source.readValue(null));
            try {
                return new InAppMessage(decodedAction);
            } catch (MalformedMessageException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public InAppMessage[] newArray(int size) {
            return new InAppMessage[size];
        }
    };
}
