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
package com.snapyr.sdk.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.text.HtmlCompat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.firebase.installations.FirebaseInstallations;
import com.snapyr.sdk.Snapyr;
import com.snapyr.sdk.Traits;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import java.util.List;

public class MainActivity extends Activity {
    @BindView(R.id.user_id)
    EditText userId;

    /** Returns true if the string is null, or empty (when trimmed). */
    public static boolean isNullOrEmpty(String text) {
        return TextUtils.isEmpty(text) || text.trim().length() == 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            handleOpenIntent(intent);
        }

        FirebaseInstallations.getInstance().delete();

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    public void handleOpenIntent(Intent intent) {
        String action = intent.getAction();
        Uri data = intent.getData();
        if (data == null) {
            Toast.makeText(this, "No deep link info provided", Toast.LENGTH_LONG).show();
            return;
        }
        List<String> paths = data.getPathSegments();
        if (paths.size() > 1) {
            String response = paths.get(0);
            String text = paths.get(1);
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            Log.e("Snapyr", "Sample app open intent data: " + data);
        }
    }

    @OnClick(R.id.action_track_a)
    void onButtonAClicked() {
        Snapyr.with(this).track("Button A Clicked");
    }

    @OnClick(R.id.action_track_b)
    void onButtonBClicked() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        WebView v = new WebView(this);
        v.canGoBackOrForward(0);
        v.layout(0,0,height,width);

        String  rawHtml = "\u003c!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional //EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"\u003e\u003chtml xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\u003e\u003chead\u003e\n\u003c!--[if gte mso 9]\u003e\n\u003cxml\u003e\n  \u003co:OfficeDocumentSettings\u003e\n    \u003co:AllowPNG/\u003e\n    \u003co:PixelsPerInch\u003e96\u003c/o:PixelsPerInch\u003e\n  \u003c/o:OfficeDocumentSettings\u003e\n\u003c/xml\u003e\n\u003c![endif]--\u003e\n  \u003cmeta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/\u003e\n  \u003cmeta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/\u003e\n  \u003cmeta name=\"x-apple-disable-message-reformatting\"/\u003e\n  \u003c!--[if !mso]\u003e\u003c!--\u003e\u003cmeta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"/\u003e\u003c!--\u003c![endif]--\u003e\n  \u003ctitle\u003e\u003c/title\u003e\n  \n    \u003cstyle type=\"text/css\"\u003e\n      @media only screen and (min-width: 520px) {\n  .u-row {\n    width: 500px !important;\n  }\n  .u-row .u-col {\n    vertical-align: top;\n  }\n\n  .u-row .u-col-100 {\n    width: 500px !important;\n  }\n\n}\n\n@media (max-width: 520px) {\n  .u-row-container {\n    max-width: 100% !important;\n    padding-left: 0px !important;\n    padding-right: 0px !important;\n  }\n  .u-row .u-col {\n    min-width: 320px !important;\n    max-width: 100% !important;\n    display: block !important;\n  }\n  .u-row {\n    width: calc(100% - 40px) !important;\n  }\n  .u-col {\n    width: 100% !important;\n  }\n  .u-col \u003e div {\n    margin: 0 auto;\n  }\n}\nbody {\n  margin: 0;\n  padding: 0;\n}\n\ntable,\ntr,\ntd {\n  vertical-align: top;\n  border-collapse: collapse;\n}\n\np {\n  margin: 0;\n}\n\n.ie-container table,\n.mso-container table {\n  table-layout: fixed;\n}\n\n* {\n  line-height: inherit;\n}\n\na[x-apple-data-detectors='true'] {\n  color: inherit !important;\n  text-decoration: none !important;\n}\n\ntable, td { color: #7b858b; } a { color: #0285ff; text-decoration: underline; } @media (max-width: 480px) { #u_content_image_1 .v-src-width { width: auto !important; } #u_content_image_1 .v-src-max-width { max-width: 59% !important; } }\n    \u003c/style\u003e\n  \n  \n\n\u003c/head\u003e\n\n\u003cbody class=\"clean-body u_body\" style=\"margin: 0;padding: 0;-webkit-text-size-adjust: 100%;background-color: #ffffff;color: #7b858b\"\u003e\n  \u003c!--[if IE]\u003e\u003cdiv class=\"ie-container\"\u003e\u003c![endif]--\u003e\n  \u003c!--[if mso]\u003e\u003cdiv class=\"mso-container\"\u003e\u003c![endif]--\u003e\n  \u003ctable style=\"border-collapse: collapse;table-layout: fixed;border-spacing: 0;mso-table-lspace: 0pt;mso-table-rspace: 0pt;vertical-align: top;min-width: 320px;Margin: 0 auto;background-color: #ffffff;width:100%\" cellpadding=\"0\" cellspacing=\"0\"\u003e\n  \u003ctbody\u003e\n  \u003ctr style=\"vertical-align: top\"\u003e\n    \u003ctd style=\"word-break: break-word;border-collapse: collapse !important;vertical-align: top\"\u003e\n    \u003c!--[if (mso)|(IE)]\u003e\u003ctable width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\u003e\u003ctr\u003e\u003ctd align=\"center\" style=\"background-color: #ffffff;\"\u003e\u003c![endif]--\u003e\n    \n\n\u003cdiv class=\"u-row-container\" style=\"padding: 0px;background-color: transparent\"\u003e\n  \u003cdiv class=\"u-row\" style=\"Margin: 0 auto;min-width: 320px;max-width: 500px;overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: transparent;\"\u003e\n    \u003cdiv style=\"border-collapse: collapse;display: table;width: 100%;background-color: transparent;\"\u003e\n      \u003c!--[if (mso)|(IE)]\u003e\u003ctable width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\u003e\u003ctr\u003e\u003ctd style=\"padding: 0px;background-color: transparent;\" align=\"center\"\u003e\u003ctable cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:500px;\"\u003e\u003ctr style=\"background-color: transparent;\"\u003e\u003c![endif]--\u003e\n      \n\u003c!--[if (mso)|(IE)]\u003e\u003ctd align=\"center\" width=\"500\" style=\"width: 500px;padding: 0px;border-top: 0px solid transparent;border-left: 0px solid transparent;border-right: 0px solid transparent;border-bottom: 0px solid transparent;\" valign=\"top\"\u003e\u003c![endif]--\u003e\n\u003cdiv class=\"u-col u-col-100\" style=\"max-width: 320px;min-width: 500px;display: table-cell;vertical-align: top;\"\u003e\n  \u003cdiv style=\"width: 100% !important;\"\u003e\n  \u003c!--[if (!mso)\u0026(!IE)]\u003e\u003c!--\u003e\u003cdiv style=\"padding: 0px;border-top: 0px solid transparent;border-left: 0px solid transparent;border-right: 0px solid transparent;border-bottom: 0px solid transparent;\"\u003e\u003c!--\u003c![endif]--\u003e\n  \n\u003ctable id=\"u_content_image_1\" style=\"font-family:arial,helvetica,sans-serif;\" role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" border=\"0\"\u003e\n  \u003ctbody\u003e\n    \u003ctr\u003e\n      \u003ctd style=\"overflow-wrap:break-word;word-break:break-word;padding:40px;font-family:arial,helvetica,sans-serif;\" align=\"left\"\u003e\n        \n\u003ctable width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\u003e\n  \u003ctbody\u003e\u003ctr\u003e\n    \u003ctd style=\"padding-right: 0px;padding-left: 0px;\" align=\"center\"\u003e\n      \u003ca target=\"_blank\" href=\"http://collx.app\"\u003e\n      \u003cimg align=\"center\" border=\"0\" src=\"https://s3.amazonaws.com/assets-qylknnlc/1640043428062-bdb7c7a5-99b2-72a1-591c-6c9092f0ccea.png\" alt=\"\" title=\"\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: inline-block !important;border: none;height: auto;float: none;width: 20%;max-width: 84px;\" width=\"84\" class=\"v-src-width v-src-max-width\"/\u003e\n      \u003c/a\u003e\n    \u003c/td\u003e\n  \u003c/tr\u003e\n\u003c/tbody\u003e\u003c/table\u003e\n\n      \u003c/td\u003e\n    \u003c/tr\u003e\n  \u003c/tbody\u003e\n\u003c/table\u003e\n\n  \u003c!--[if (!mso)\u0026(!IE)]\u003e\u003c!--\u003e\u003c/div\u003e\u003c!--\u003c![endif]--\u003e\n  \u003c/div\u003e\n\u003c/div\u003e\n\u003c!--[if (mso)|(IE)]\u003e\u003c/td\u003e\u003c![endif]--\u003e\n      \u003c!--[if (mso)|(IE)]\u003e\u003c/tr\u003e\u003c/table\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/table\u003e\u003c![endif]--\u003e\n    \u003c/div\u003e\n  \u003c/div\u003e\n\u003c/div\u003e\n\n\n\n\u003cdiv class=\"u-row-container\" style=\"padding: 0px;background-color: transparent\"\u003e\n  \u003cdiv class=\"u-row\" style=\"Margin: 0 auto;min-width: 320px;max-width: 500px;overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: transparent;\"\u003e\n    \u003cdiv style=\"border-collapse: collapse;display: table;width: 100%;background-color: transparent;\"\u003e\n      \u003c!--[if (mso)|(IE)]\u003e\u003ctable width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\u003e\u003ctr\u003e\u003ctd style=\"padding: 0px;background-color: transparent;\" align=\"center\"\u003e\u003ctable cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:500px;\"\u003e\u003ctr style=\"background-color: transparent;\"\u003e\u003c![endif]--\u003e\n      \n\u003c!--[if (mso)|(IE)]\u003e\u003ctd align=\"center\" width=\"500\" style=\"background-color: #f2f8fa;width: 500px;padding: 0px;border-top: 0px solid transparent;border-left: 0px solid transparent;border-right: 0px solid transparent;border-bottom: 0px solid transparent;border-radius: 0px;-webkit-border-radius: 0px; -moz-border-radius: 0px;\" valign=\"top\"\u003e\u003c![endif]--\u003e\n\u003cdiv class=\"u-col u-col-100\" style=\"max-width: 320px;min-width: 500px;display: table-cell;vertical-align: top;\"\u003e\n  \u003cdiv style=\"background-color: #f2f8fa;width: 100% !important;border-radius: 0px;-webkit-border-radius: 0px; -moz-border-radius: 0px;\"\u003e\n  \u003c!--[if (!mso)\u0026(!IE)]\u003e\u003c!--\u003e\u003cdiv style=\"padding: 0px;border-top: 0px solid transparent;border-left: 0px solid transparent;border-right: 0px solid transparent;border-bottom: 0px solid transparent;border-radius: 0px;-webkit-border-radius: 0px; -moz-border-radius: 0px;\"\u003e\u003c!--\u003c![endif]--\u003e\n  \n\u003ctable style=\"font-family:arial,helvetica,sans-serif;\" role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" border=\"0\"\u003e\n  \u003ctbody\u003e\n    \u003ctr\u003e\n      \u003ctd style=\"overflow-wrap:break-word;word-break:break-word;padding:10px;font-family:arial,helvetica,sans-serif;\" align=\"left\"\u003e\n        \n  \u003cdiv style=\"line-height: 140%; text-align: left; word-wrap: break-word;\"\u003e\n    \u003cp style=\"font-size: 14px; line-height: 140%;\"\u003e🎊 You got 5 referrals and a free pack! 🎊\u003c/p\u003e\n  \u003c/div\u003e\n\n      \u003c/td\u003e\n    \u003c/tr\u003e\n  \u003c/tbody\u003e\n\u003c/table\u003e\n\n\u003ctable style=\"font-family:arial,helvetica,sans-serif;\" role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" border=\"0\"\u003e\n  \u003ctbody\u003e\n    \u003ctr\u003e\n      \u003ctd style=\"overflow-wrap:break-word;word-break:break-word;padding:10px;font-family:arial,helvetica,sans-serif;\" align=\"left\"\u003e\n        \n\u003ctable width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\u003e\n  \u003ctbody\u003e\u003ctr\u003e\n    \u003ctd style=\"padding-right: 0px;padding-left: 0px;\" align=\"center\"\u003e\n      \u003ca target=\"_blank\" href=\"collx://profiles/\"\u003e\n      \u003cimg align=\"center\" border=\"0\" src=\"http://collx1\" alt=\"\" title=\"\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: inline-block !important;border: none;height: auto;float: none;width: 25%;max-width: 120px;\" width=\"120\" class=\"v-src-width v-src-max-width\"/\u003e\n      \u003c/a\u003e\n    \u003c/td\u003e\n  \u003c/tr\u003e\n\u003c/tbody\u003e\u003c/table\u003e\n\n      \u003c/td\u003e\n    \u003c/tr\u003e\n  \u003c/tbody\u003e\n\u003c/table\u003e\n\n\u003ctable style=\"font-family:arial,helvetica,sans-serif;\" role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" border=\"0\"\u003e\n  \u003ctbody\u003e\n    \u003ctr\u003e\n      \u003ctd style=\"overflow-wrap:break-word;word-break:break-word;padding:10px;font-family:arial,helvetica,sans-serif;\" align=\"left\"\u003e\n        \n\u003ctable width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\u003e\n  \u003ctbody\u003e\u003ctr\u003e\n    \u003ctd style=\"padding-right: 0px;padding-left: 0px;\" align=\"center\"\u003e\n      \u003ca target=\"_blank\" href=\"collx://profiles/\"\u003e\n      \u003cimg align=\"center\" border=\"0\" src=\"http://collx2\" alt=\"\" title=\"\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: inline-block !important;border: none;height: auto;float: none;width: 25%;max-width: 120px;\" width=\"120\" class=\"v-src-width v-src-max-width\"/\u003e\n      \u003c/a\u003e\n    \u003c/td\u003e\n  \u003c/tr\u003e\n\u003c/tbody\u003e\u003c/table\u003e\n\n      \u003c/td\u003e\n    \u003c/tr\u003e\n  \u003c/tbody\u003e\n\u003c/table\u003e\n\n\u003ctable style=\"font-family:arial,helvetica,sans-serif;\" role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" border=\"0\"\u003e\n  \u003ctbody\u003e\n    \u003ctr\u003e\n      \u003ctd style=\"overflow-wrap:break-word;word-break:break-word;padding:10px;font-family:arial,helvetica,sans-serif;\" align=\"left\"\u003e\n        \n\u003ctable width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\u003e\n  \u003ctbody\u003e\u003ctr\u003e\n    \u003ctd style=\"padding-right: 0px;padding-left: 0px;\" align=\"center\"\u003e\n      \u003ca target=\"_blank\" href=\"collx://profiles/\"\u003e\n      \u003cimg align=\"center\" border=\"0\" src=\"http://collx3\" alt=\"\" title=\"\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: inline-block !important;border: none;height: auto;float: none;width: 25%;max-width: 120px;\" width=\"120\" class=\"v-src-width v-src-max-width\"/\u003e\n      \u003c/a\u003e\n    \u003c/td\u003e\n  \u003c/tr\u003e\n\u003c/tbody\u003e\u003c/table\u003e\n\n      \u003c/td\u003e\n    \u003c/tr\u003e\n  \u003c/tbody\u003e\n\u003c/table\u003e\n\n\u003ctable style=\"font-family:arial,helvetica,sans-serif;\" role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" border=\"0\"\u003e\n  \u003ctbody\u003e\n    \u003ctr\u003e\n      \u003ctd style=\"overflow-wrap:break-word;word-break:break-word;padding:10px;font-family:arial,helvetica,sans-serif;\" align=\"left\"\u003e\n        \n\u003ctable width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\u003e\n  \u003ctbody\u003e\u003ctr\u003e\n    \u003ctd style=\"padding-right: 0px;padding-left: 0px;\" align=\"center\"\u003e\n      \u003ca target=\"_blank\" href=\"collx://profiles/\"\u003e\n      \u003cimg align=\"center\" border=\"0\" src=\"http://collx4\" alt=\"\" title=\"\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: inline-block !important;border: none;height: auto;float: none;width: 25%;max-width: 120px;\" width=\"120\" class=\"v-src-width v-src-max-width\"/\u003e\n      \u003c/a\u003e\n    \u003c/td\u003e\n  \u003c/tr\u003e\n\u003c/tbody\u003e\u003c/table\u003e\n\n      \u003c/td\u003e\n    \u003c/tr\u003e\n  \u003c/tbody\u003e\n\u003c/table\u003e\n\n\u003ctable style=\"font-family:arial,helvetica,sans-serif;\" role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" border=\"0\"\u003e\n  \u003ctbody\u003e\n    \u003ctr\u003e\n      \u003ctd style=\"overflow-wrap:break-word;word-break:break-word;padding:10px;font-family:arial,helvetica,sans-serif;\" align=\"left\"\u003e\n        \n\u003ctable width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\u003e\n  \u003ctbody\u003e\u003ctr\u003e\n    \u003ctd style=\"padding-right: 0px;padding-left: 0px;\" align=\"center\"\u003e\n      \u003ca target=\"_blank\" href=\"collx://profiles/\"\u003e\n      \u003cimg align=\"center\" border=\"0\" src=\"http://collx5\" alt=\"\" title=\"\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: inline-block !important;border: none;height: auto;float: none;width: 25%;max-width: 120px;\" width=\"120\" class=\"v-src-width v-src-max-width\"/\u003e\n      \u003c/a\u003e\n    \u003c/td\u003e\n  \u003c/tr\u003e\n\u003c/tbody\u003e\u003c/table\u003e\n\n      \u003c/td\u003e\n    \u003c/tr\u003e\n  \u003c/tbody\u003e\n\u003c/table\u003e\n\n\u003ctable style=\"font-family:arial,helvetica,sans-serif;\" role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" border=\"0\"\u003e\n  \u003ctbody\u003e\n    \u003ctr\u003e\n      \u003ctd style=\"overflow-wrap:break-word;word-break:break-word;padding:10px;font-family:arial,helvetica,sans-serif;\" align=\"left\"\u003e\n        \n  \u003cdiv style=\"line-height: 140%; text-align: left; word-wrap: break-word;\"\u003e\n    \u003cp style=\"font-size: 14px; line-height: 140%;\"\u003eCongratulations! You referred 5 people to CollX and earned a free pack of cards. Head to the Referral screen from your Profile to pick what sport and send us your address.\u003c/p\u003e\n\u003cp style=\"font-size: 14px; line-height: 140%;\"\u003e \u003c/p\u003e\n\u003cp style=\"font-size: 14px; line-height: 140%;\"\u003eThank you so much for getting the word out about CollX!\u003c/p\u003e\n  \u003c/div\u003e\n\n      \u003c/td\u003e\n    \u003c/tr\u003e\n  \u003c/tbody\u003e\n\u003c/table\u003e\n\n\u003ctable style=\"font-family:arial,helvetica,sans-serif;\" role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" border=\"0\"\u003e\n  \u003ctbody\u003e\n    \u003ctr\u003e\n      \u003ctd style=\"overflow-wrap:break-word;word-break:break-word;padding:10px;font-family:arial,helvetica,sans-serif;\" align=\"left\"\u003e\n        \n\u003cdiv align=\"center\"\u003e\n  \u003c!--[if mso]\u003e\u003ctable width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-spacing: 0; border-collapse: collapse; mso-table-lspace:0pt; mso-table-rspace:0pt;font-family:arial,helvetica,sans-serif;\"\u003e\u003ctr\u003e\u003ctd style=\"font-family:arial,helvetica,sans-serif;\" align=\"center\"\u003e\u003cv:roundrect xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:w=\"urn:schemas-microsoft-com:office:word\" href=\"collx://profile\" style=\"height:37px; v-text-anchor:middle; width:112px;\" arcsize=\"11%\" stroke=\"f\" fillcolor=\"#3AAEE0\"\u003e\u003cw:anchorlock/\u003e\u003ccenter style=\"color:#FFFFFF;font-family:arial,helvetica,sans-serif;\"\u003e\u003c![endif]--\u003e\n    \u003ca target=\"_blank\" style=\"box-sizing: border-box;display: inline-block;font-family:arial,helvetica,sans-serif;text-decoration: none;-webkit-text-size-adjust: none;text-align: center;color: #FFFFFF; background-color: #3AAEE0; border-radius: 4px;-webkit-border-radius: 4px; -moz-border-radius: 4px; width:auto; max-width:100%; overflow-wrap: break-word; word-break: break-word; word-wrap:break-word; mso-border-alt: none;\" href=\"collx://profile\"\u003e\n      \u003cspan style=\"display:block;padding:10px 20px;line-height:120%;\"\u003e\u003cspan style=\"font-size: 14px; line-height: 16.8px;\"\u003eYour Profile\u003c/span\u003e\u003c/span\u003e\n    \u003c/a\u003e\n  \u003c!--[if mso]\u003e\u003c/center\u003e\u003c/v:roundrect\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/table\u003e\u003c![endif]--\u003e\n\u003c/div\u003e\n\n      \u003c/td\u003e\n    \u003c/tr\u003e\n  \u003c/tbody\u003e\n\u003c/table\u003e\n\n  \u003c!--[if (!mso)\u0026(!IE)]\u003e\u003c!--\u003e\u003c/div\u003e\u003c!--\u003c![endif]--\u003e\n  \u003c/div\u003e\n\u003c/div\u003e\n\u003c!--[if (mso)|(IE)]\u003e\u003c/td\u003e\u003c![endif]--\u003e\n      \u003c!--[if (mso)|(IE)]\u003e\u003c/tr\u003e\u003c/table\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/table\u003e\u003c![endif]--\u003e\n    \u003c/div\u003e\n  \u003c/div\u003e\n\u003c/div\u003e\n\n\n\n\u003cdiv class=\"u-row-container\" style=\"padding: 0px;background-color: transparent\"\u003e\n  \u003cdiv class=\"u-row\" style=\"Margin: 0 auto;min-width: 320px;max-width: 500px;overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: transparent;\"\u003e\n    \u003cdiv style=\"border-collapse: collapse;display: table;width: 100%;background-color: transparent;\"\u003e\n      \u003c!--[if (mso)|(IE)]\u003e\u003ctable width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\u003e\u003ctr\u003e\u003ctd style=\"padding: 0px;background-color: transparent;\" align=\"center\"\u003e\u003ctable cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:500px;\"\u003e\u003ctr style=\"background-color: transparent;\"\u003e\u003c![endif]--\u003e\n      \n\u003c!--[if (mso)|(IE)]\u003e\u003ctd align=\"center\" width=\"500\" style=\"background-color: #ffffff;width: 500px;padding: 0px;border-top: 0px solid transparent;border-left: 0px solid transparent;border-right: 0px solid transparent;border-bottom: 0px solid transparent;border-radius: 0px;-webkit-border-radius: 0px; -moz-border-radius: 0px;\" valign=\"top\"\u003e\u003c![endif]--\u003e\n\u003cdiv class=\"u-col u-col-100\" style=\"max-width: 320px;min-width: 500px;display: table-cell;vertical-align: top;\"\u003e\n  \u003cdiv style=\"background-color: #ffffff;width: 100% !important;border-radius: 0px;-webkit-border-radius: 0px; -moz-border-radius: 0px;\"\u003e\n  \u003c!--[if (!mso)\u0026(!IE)]\u003e\u003c!--\u003e\u003cdiv style=\"padding: 0px;border-top: 0px solid transparent;border-left: 0px solid transparent;border-right: 0px solid transparent;border-bottom: 0px solid transparent;border-radius: 0px;-webkit-border-radius: 0px; -moz-border-radius: 0px;\"\u003e\u003c!--\u003c![endif]--\u003e\n  \n\u003ctable style=\"font-family:arial,helvetica,sans-serif;\" role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" border=\"0\"\u003e\n  \u003ctbody\u003e\n    \u003ctr\u003e\n      \u003ctd style=\"overflow-wrap:break-word;word-break:break-word;padding:10px;font-family:arial,helvetica,sans-serif;\" align=\"left\"\u003e\n        \n  \u003cdiv style=\"line-height: 140%; text-align: left; word-wrap: break-word;\"\u003e\n    \u003cp style=\"font-size: 14px; line-height: 140%; text-align: center;\"\u003e\u003cem\u003e\u003cspan style=\"font-size: 12px; line-height: 16.799999999999997px;\"\u003eCopyright © 2022 CollX, All rights reserved.\u003c/span\u003e\u003c/em\u003e\u003cbr/\u003e\u003cspan style=\"font-size: 12px; line-height: 16.799999999999997px;\"\u003eYou are receiving this email because you opted in via the CollX app.\u003c/span\u003e\u003c/p\u003e\n\u003cp style=\"font-size: 14px; line-height: 140%; text-align: center;\"\u003e \u003c/p\u003e\n\u003cp style=\"font-size: 14px; line-height: 140%; text-align: center;\"\u003e\u003cstrong\u003e\u003cspan style=\"font-size: 12px; line-height: 16.799999999999997px;\"\u003eOur mailing address is:\u003c/span\u003e\u003c/strong\u003e\u003cbr/\u003e\u003cspan style=\"font-size: 12px; line-height: 16.799999999999997px;\"\u003eCollX, 207 N. Haddon Ave., Haddonfield, NJ 08033\u003c/span\u003e\u003c/p\u003e\n\u003cp style=\"font-size: 14px; line-height: 140%; text-align: center;\"\u003e\u003cspan style=\"font-size: 12px; line-height: 16.799999999999997px;\"\u003e\u003c/span\u003e\u003cbr/\u003e\u003cspan style=\"font-size: 12px; line-height: 16.799999999999997px;\"\u003e{{{ pm:unsubscribe }}}\u003c/span\u003e\u003cbr/\u003e\u003cspan style=\"font-size: 12px; line-height: 16.799999999999997px;\"\u003e\u003c/span\u003e\u003c/p\u003e\n  \u003c/div\u003e\n\n      \u003c/td\u003e\n    \u003c/tr\u003e\n  \u003c/tbody\u003e\n\u003c/table\u003e\n\n  \u003c!--[if (!mso)\u0026(!IE)]\u003e\u003c!--\u003e\u003c/div\u003e\u003c!--\u003c![endif]--\u003e\n  \u003c/div\u003e\n\u003c/div\u003e\n\u003c!--[if (mso)|(IE)]\u003e\u003c/td\u003e\u003c![endif]--\u003e\n      \u003c!--[if (mso)|(IE)]\u003e\u003c/tr\u003e\u003c/table\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/table\u003e\u003c![endif]--\u003e\n    \u003c/div\u003e\n  \u003c/div\u003e\n\u003c/div\u003e\n\n\n    \u003c!--[if (mso)|(IE)]\u003e\u003c/td\u003e\u003c/tr\u003e\u003c/table\u003e\u003c![endif]--\u003e\n    \u003c/td\u003e\n  \u003c/tr\u003e\n  \u003c/tbody\u003e\n  \u003c/table\u003e\n  \u003c!--[if mso]\u003e\u003c/div\u003e\u003c![endif]--\u003e\n  \u003c!--[if IE]\u003e\u003c/div\u003e\u003c![endif]--\u003e\n\n\n\n\u003cimg src=\"https://engine.snapyr.com/v1/tracking/pixel.gif?token=NjFmNWE3ZTMtZDQwNi00MzhkLThlYzQtMGQyYmRlMGMyMWY2OmYzZGI3MTJlLWRhNTYtNDkwYy04NjE2LTM5MzI2ZjE3NDk4YzoxMjU1YTIyMC1kNDI0LTQzYTEtYWVlYi1jZGM3MTVkYzg0ZDE6ZDI3ZDMxODctN2Y3NS00MGQzLWIwMTUtNjExZjU3ZjIzYmYyOnB1Ymxpc2g6ODgwMWFmYzItOTJiOS00YjQ1LWI1NTQtMDkxMTliNzYzMzNjOjY3OnRiZDoxNjU5NjIzNTI4OmFjdGlvblRva2VuLTg4MDFhZmMyLTkyYjktNGI0NS1iNTU0LTA5MTE5Yjc2MzMzYy42Ny4xNjU5NjIzNTI4\" height=\"1\" border=\"0\" style=\"height:1px !important;width:1px !important;border-width:0 !important;margin-top:0 !important;margin-bottom:0 !important;margin-right:0 !important;margin-left:0 !important;padding-top:0 !important;padding-bottom:0 !important;padding-right:0 !important;padding-left:0 !important;\"/\u003e\u003c/body\u003e\u003c/html\u003e";
        String encodedHtml = Base64.encodeToString(rawHtml.getBytes(), Base64.NO_PADDING);

        v.loadData(encodedHtml, "text/html", "base64");

        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
        dialog.setMessage("Brandon is cool");
        dialog.show();
        Snapyr.with(this).track("Button B Clicked");
    }

    @OnClick(R.id.action_show_notification)
    void onShowNotifyClicked() {
        Snapyr.with(this).getNotificationHandler().showSampleNotification();
    }

    @OnClick(R.id.action_identify)
    void onIdentifyButtonClicked() {
        String id = userId.getText().toString();
        if (isNullOrEmpty(id)) {
            Toast.makeText(this, R.string.id_required, Toast.LENGTH_LONG).show();
        } else {
            Traits traits = new Traits().putValue("testAmount", 100);
            Snapyr.with(this).identify(id, traits, null);
        }
    }

    @OnClick(R.id.action_flush)
    void onFlushButtonClicked() {
        Snapyr.with(this).flush();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_view_docs) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://snapyr.com/docs/"));
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.no_browser_available, Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

}
