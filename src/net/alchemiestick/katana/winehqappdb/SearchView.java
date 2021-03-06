/*
    Copyright 2012 Rene Kjellerup
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.alchemiestick.katana.winehqappdb;

import java.util.*;

import android.app.*;
import android.content.*;
import android.content.DialogInterface.*;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import android.widget.TextView.*;

import org.apache.http.*;
import org.apache.http.message.*;

public class SearchView extends Activity
{
    static public final int UNLICENSED  = 0x0004400;
    static public final int ABOUT_DLG   = 0x0004500;
    static public final int FILTERS_DLG = 0x0004600;
    static public final int WINAPP_DLG  = 0x0004700;
    static public Context app_cx;
    
    public TextView  input;
    public List<NameValuePair> webData;

    public ApplicationList applist;

    private Handler uiHnd;

    public void do_search(WineSearch ws) {
        setAppNameData(input.getText().toString());
        ws.execute(ws.getCall("https://appdb.winehq.org/objectManager.php"));
    }

    public static void do_sleep(int msec) {
        try {
            Thread.currentThread().sleep(1500);
        } catch (Exception e) {}
    }
    
    private View.OnClickListener searchClick = new View.OnClickListener() {
        public void onClick(View v) {
            SearchView cx = (SearchView)v.getContext();
            WineSearch appdb = new WineSearch(cx);
            cx.do_search(appdb);
        }
    };
    
    private OnEditorActionListener inputEnter = new OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            boolean handled = false;
            SearchView cx = (SearchView)v.getContext();
            if ( actionId == EditorInfo.IME_ACTION_SEARCH ) {
                InputMethodManager imm = (InputMethodManager)cx.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                WineSearch appdb = new WineSearch(cx);
                cx.do_search(appdb);
                handled = true;
            }
            return handled;
        }
    };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.app_cx = this;
        setContentView(R.layout.main);

        webData = new ArrayList<NameValuePair>();
        setDefaults();

        input = (EditText)findViewById(R.id.searchInput);
        input.setOnEditorActionListener(inputEnter);

        Button s = (Button)findViewById(R.id.searchSubmit);
        s.setOnClickListener(this.searchClick);

        applist = new ApplicationList(this);

        ListView lv = (ListView)findViewById(R.id.list);
        lv.setAdapter(applist);

        // send the usage count to the usage metrics store
        Metrics mmc = new Metrics(this);
        mmc.execute(mmc.getCall());
    }
    
    /* showing / creating the menu */
    @Override
    public boolean onCreateOptionsMenu(Menu m)
    {
        MenuInflater inf = getMenuInflater();
        inf.inflate(R.menu.app_menu, m);
        return true;
    }

    /* dealing with the user's selection */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
    switch (item.getItemId()) {
            case R.id.about:
                showDialog(ABOUT_DLG);
                return true;
            case R.id.filters:
                // still to be done. based on setDefaults();
                showDialog(FILTERS_DLG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog diag;
        switch(id) {
        case ABOUT_DLG:
            diag = about_dialog();
            break;
        case FILTERS_DLG:
            // TODO: create the real filters dialog and have it modify webData accordingly.
            diag = new filter_dialog(this, this.webData);
            break;
        case WINAPP_DLG:
            diag = applist.makeDialog();
            break;
        default:
            diag = null;
        }
        return diag;
    }

    private Dialog about_dialog()
    {
        String msg = "Copyright May 25th 2012 by Rene Kjellerup (aka Katana Steel) and Alchemiestick.\n\n";
        msg += "WineHQ Appdb Search is released under GPLv3 or later. It uses images from WINE project under LGPLv2 or later ";
        msg += "see license:\nhttp://www.gnu.org/licenses/\nfor more infomation about the licenses.\n\n";
        msg += "Souce code for the program can be obtained at\nhttps://github.com/Katana-Steel/winehqappdb\nand choose ";
        msg += "the apropriate release tag for the the version you are running.";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About")
        .setMessage(msg)
        .setCancelable(true)
        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        return builder.create();
    }
    
    private void setNamedData(String name, String value) 
    {
        ListIterator<NameValuePair> itr = webData.listIterator();
        while(itr.hasNext())
        {
            NameValuePair t = itr.next();
            if(t.getName() == name) {
                itr.set(new BasicNameValuePair(name, value));
                return;
            }
        }
        webData.add(new BasicNameValuePair(name, value));
    }
    
    private void removeNamedData(String name) 
    {
        ListIterator<NameValuePair> itr = webData.listIterator();
        while(itr.hasNext())
        {
            NameValuePair t = itr.next();
            if(t.getName() == name) {
                itr.remove();
                return;
            }
        }
    }

    private void setAppNameData(String name) 
    {
        setNamedData("sappFamily-appNameData", name);
    }
    
    public void setDefaults()
    {
        webData.clear();
        /* the GET Data from the url */
        setNamedData("bIsQueue", "false");
        setNamedData("sClass", "application");  // this is what we are looking for
        setNamedData("sTitle", "Browse+Applications");
        setNamedData("iItemsPerPage", "30"); // get this many items at a time
        setNamedData("iPage", "1");  // first page
        setNamedData("sOrderBy", "appName");
        setNamedData("bAscending", "true");
        /* the default POST data from the web form */
        setNamedData("iappVersion-ratingOp", "5");
        setNamedData("iappCategoryOp", "11");
        setNamedData("iappVersion-licenseOp", "5");
        setNamedData("sappVersion-ratingData", ""); // Platinum, Gold, Silver, Bronze, Garbage 
        setNamedData("iversions-idOp", ""); // 5 =,6 <, 7 >
        setNamedData("sversions-idData", ""); // > 242 & < 131
        setNamedData("sappCategoryData", ""); // short int 1-999 (i think)
        setNamedData("sappVersion-licenseData", ""); // Retail, Open Source, Free to use, Free to use and share, Demo, Shareware
        setNamedData("iappFamily-appNameOp", "2"); // 2 = contains, 3 = starts with, 4 = ends with
        setNamedData("ionlyDownloadableOp", "10"); 
        setNamedData("sFilterSubmit", "Update Filter"); // the web submit button
        // setNamedData("sonlyDownloadableData", "true"); // unchecked by default
    }
}
