package com.freekiosk;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Runs the actual release application payload with a controlled loopback webpage. */
public class EmbeddedChromiumInstrumentation extends Instrumentation {
 private String expectedVersion;
 @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
 private WebView findWebView(View view) {
  if(view instanceof WebView) return (WebView)view;
  if(view instanceof ViewGroup) {
   ViewGroup group=(ViewGroup)view;
   for(int i=0;i<group.getChildCount();i++) { WebView v=findWebView(group.getChildAt(i)); if(v!=null)return v; }
  }
  return null;
 }
 private void put(SQLiteDatabase db,String key,String value) {
  ContentValues row=new ContentValues();row.put("key",key);row.put("value",value);
  db.insertWithOnConflict("catalystLocalStorage",null,row,SQLiteDatabase.CONFLICT_REPLACE);
 }
 private void fixtureConfig(Context context) throws Exception {
  File file=context.getDatabasePath("RKStorage");file.getParentFile().mkdirs();
  try(SQLiteDatabase db=SQLiteDatabase.openOrCreateDatabase(file,null)) {
   db.execSQL("CREATE TABLE IF NOT EXISTS catalystLocalStorage (key TEXT PRIMARY KEY, value TEXT)");db.setVersion(1);
   put(db,"@kiosk_url","http://127.0.0.1:8765/");put(db,"@kiosk_enabled","false");put(db,"@kiosk_allow_hardware_screenshot","true");put(db,"@kiosk_auto_reload","false");
  }
  try(java.io.InputStream in=context.getAssets().open("embedded-chromium.json")) {
   java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
   while((n=in.read(buf))!=-1)out.write(buf,0,n);
   expectedVersion=new JSONObject(out.toString("UTF-8")).getString("version");
  }
 }
 @Override public void onStart() {
  Bundle report=new Bundle();
  try {
   Context context=getTargetContext();fixtureConfig(context);
   Activity activity=startActivitySync(new Intent().setClassName(context.getPackageName(),context.getPackageName()+".MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   long deadline=System.currentTimeMillis()+240000;JSONObject payload=null;String last="No WebView";
   while(System.currentTimeMillis()<deadline && payload==null) {
    CountDownLatch done=new CountDownLatch(1);AtomicReference<String> value=new AtomicReference<>();
    runOnMainSync(()->{
     WebView view=findWebView(activity.getWindow().getDecorView());
     if(view==null){done.countDown();return;}
     view.evaluateJavascript("JSON.stringify(window.__easyKioskResult || null)",response->{value.set(response);done.countDown();});
    });
    done.await(3,TimeUnit.SECONDS);
    if(value.get()!=null) {
     last=value.get();
     try {Object decoded=new JSONTokener(last).nextValue();if(decoded instanceof String && !decoded.equals("null"))payload=new JSONObject((String)decoded);}catch(Exception ignored){}
    }
    if(payload==null)Thread.sleep(250);
   }
   if(payload==null)throw new AssertionError("Full app did not render fixture: "+last);
   String major=expectedVersion.substring(0,expectedVersion.indexOf('.'));
   if(!payload.getString("ua").contains("Chrome/"+major+".") || !payload.getBoolean("js") || !payload.getBoolean("css") || !payload.getBoolean("net") || !payload.getBoolean("bridge"))throw new AssertionError("Browser checks failed: "+payload);
   AtomicReference<String> actual=new AtomicReference<>();runOnMainSync(()->{
    if(findWebView(activity.getWindow().getDecorView()).getWebViewRenderProcess()==null)throw new AssertionError("Renderer is not out of process");
    actual.set(WebView.getCurrentWebViewPackage().versionName);});
   if(!expectedVersion.equals(actual.get()))throw new AssertionError("Wrong actual engine: "+actual.get());
   Bitmap screenshot=getUiAutomation().takeScreenshot();
   if(screenshot!=null)try(FileOutputStream out=new FileOutputStream(new File(context.getExternalFilesDir(null),"engine-smoke.png"))){screenshot.compress(Bitmap.CompressFormat.PNG,100,out);}
   report.putString("stream","EASYKIOSK_EMBEDDED_ENGINE_OK version="+actual.get()+" "+payload+"\n");finish(Activity.RESULT_OK,report);
  } catch(Throwable error) {report.putString("stream","EASYKIOSK_EMBEDDED_ENGINE_FAILED "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,report);}
 }
}
