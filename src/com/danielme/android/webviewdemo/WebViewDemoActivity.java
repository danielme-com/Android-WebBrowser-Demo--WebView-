/*
* Copyright (C) 2012 Daniel Medina  - http://danielme.com
 
   This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>

*/

package com.danielme.android.webviewdemo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class WebViewDemoActivity extends Activity
{
	private WebView webview;

	private ProgressBar progressBar;

	private EditText url;

	private List<String> historyStack;

	private ArrayAdapter<String> dialogArrayAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		historyStack = new LinkedList<String>();
		webview = (WebView) findViewById(R.id.webkit);
		url = (EditText) findViewById(R.id.url);
		progressBar = (ProgressBar) findViewById(R.id.progressbar);	

		// javascript and zoom
		webview.getSettings().setJavaScriptEnabled(true);
		webview.getSettings().setBuiltInZoomControls(true);

		// downloads
		webview.setDownloadListener(new CustomDownloadListener());

		webview.setWebViewClient(new CustomWebViewClient());					

		webview.setWebChromeClient(new WebChromeClient()
		{
			@Override
			public void onProgressChanged(WebView view, int progress)
			{
				progressBar.setProgress(0);
				FrameLayout progressBarLayout = (FrameLayout) findViewById(R.id.progressBarLayout);
				progressBarLayout.setVisibility(View.VISIBLE);
				WebViewDemoActivity.this.setProgress(progress * 1000);
				
				TextView progressStatus = (TextView) findViewById(R.id.progressStatus);
				progressStatus.setText(progress + " %");
				progressBar.incrementProgressBy(progress);

				if (progress == 100)
				{
					progressBarLayout.setVisibility(View.GONE);
				}
			}

			@Override
			public void onReceivedTitle(WebView view, String title)
			{
				WebViewDemoActivity.this.setTitle(getString(R.string.app_name) + " - " + WebViewDemoActivity.this.webview.getTitle());
			}

		});
		
		//http://stackoverflow.com/questions/2083909/android-webview-refusing-user-input
		webview.setOnTouchListener(new View.OnTouchListener() 
		{ 
			@Override
			public boolean onTouch(View v, MotionEvent event) 
			{
			           switch (event.getAction()) 
			           { 
			               case MotionEvent.ACTION_DOWN: 
			               case MotionEvent.ACTION_UP: 
			                   if (!v.hasFocus()) 
			                   { 
			                       v.requestFocus(); 
			                   }  
			                   break; 
			           } 
			           return false; 
			        }
			
			});


		// Welcome page loaded from assets directory
		if (Locale.getDefault().getLanguage().equals("es"))
		{
			webview.loadUrl("file:///android_asset/welcome_es.html");
		}
		else
		{
			webview.loadUrl("file:///android_asset/welcome.html");
		}
		
		webview.requestFocus();
	}
	
	
	@Override
	protected Dialog onCreateDialog(int id)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle(getString(R.string.history));		
		builder.setPositiveButton(getString(R.string.clear), new OnClickListener()
		{			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				historyStack.clear();				
			}
		});
		
		builder.setNegativeButton(R.string.close, null);

		dialogArrayAdapter = new ArrayAdapter<String>(this, R.layout.history, historyStack)
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				//holder pattern
				TextView holder = null;
				if (convertView == null)
				{
					LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					convertView = inflater.inflate(R.layout.history, null);
					holder = (TextView) convertView.findViewById(R.id.textView1);
					convertView.setTag(holder);
				}
				else
				{
					holder = (TextView) convertView.getTag();
				}

				holder.setText(getItem(position));
				return convertView;
			}
		};

		builder.setAdapter(dialogArrayAdapter, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int item)
			{
				webview.loadUrl(historyStack.get(item).toString());
			}

		});

		return builder.create();
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog)
	{
		dialogArrayAdapter.notifyDataSetChanged();
		super.onPrepareDialog(id, dialog);
	}

	// back button
	@Override
	public void onBackPressed()
	{
		if (webview.canGoBack())
		{
			webview.goBack();
		}
		else
		{
			super.onBackPressed();
		}
	}
	
	class CustomWebViewClient extends WebViewClient
	{
		// the current WebView will handle the url
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
			return false;
		}

		// history and navigation buttons
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon)
		{
			// shows the current url
			WebViewDemoActivity.this.url.setText(url);

			//only one occurrence
			boolean b = false;
			ListIterator<String> listIterator = historyStack.listIterator();
			while (listIterator.hasNext() && !b)
			{
				if (listIterator.next().equals(url))
				{
					b = true;
					listIterator.remove();
				}
			}
			
			historyStack.add(0, url);

			((Button) WebViewDemoActivity.this.findViewById(R.id.stopButton)).setEnabled(true);
			updateButtons();
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{
			((Button) WebViewDemoActivity.this.findViewById(R.id.stopButton)).setEnabled(false);
			updateButtons();
		}

		// handles unrecoverable errors
		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(WebViewDemoActivity.this);
			builder.setMessage(description).setPositiveButton((R.string.ok), null).setTitle("onReceivedError");
			builder.show();
		}

	}


	public void go(View view)
	{
		// hides the keyboard
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(url.getWindowToken(), 0);

		((Button) WebViewDemoActivity.this.findViewById(R.id.stopButton)).setEnabled(true);

		webview.loadUrl(url.getText().toString());
	}

	public void back(View view)
	{
		webview.goBack();
	}

	public void forward(View view)
	{
		webview.goForward();
	}

	public void stop(View view)
	{
		webview.stopLoading();
		Toast.makeText(this, getString(R.string.stopping), Toast.LENGTH_LONG).show();
	}

	public void history(View view)
	{
		showDialog(0);
	}



	private void updateButtons()
	{
		Button backButton = (Button) WebViewDemoActivity.this.findViewById(R.id.backButton);

		if (webview.canGoBack())
		{
			backButton.setEnabled(true);
		}
		else
		{
			backButton.setEnabled(false);
		}

		Button forwardButton = (Button) WebViewDemoActivity.this.findViewById(R.id.forwardButton);

		if (webview.canGoForward())
		{
			forwardButton.setEnabled(true);
		}
		else
		{
			forwardButton.setEnabled(false);
		}
	}

	// DOWNLOAD MANAGER WITH ASYNCTASK

	class CustomDownloadListener implements DownloadListener
	{
		public void onDownloadStart(final String url, String userAgent, String contentDisposition, String mimetype, long contentLength)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(WebViewDemoActivity.this);
			
			builder.setTitle(getString(R.string.download));
			builder.setMessage(getString(R.string.question));
			builder.setCancelable(false).setPositiveButton((R.string.ok), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					new DownloadAsyncTask().execute(url);
				}

			}).setNegativeButton((R.string.cancel), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					dialog.cancel();
				}
			});
			
			builder.create().show();

		}

	}

	private class DownloadAsyncTask extends AsyncTask<String, Void, String>
	{

		@Override
		protected String doInBackground(String... arg0)
		{
			String result = "";
			String url = arg0[0];
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url);
			InputStream inputStream = null;
			try
			{
				HttpResponse httpResponse = httpClient.execute(httpGet);

				BufferedHttpEntity bufferedHttpEntity = new BufferedHttpEntity(httpResponse.getEntity());

				inputStream = bufferedHttpEntity.getContent();
				
				String fileName = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/webviewdemo";
				File directory = new File(fileName);
				File file = new File(directory, url.substring(url.lastIndexOf("/")));				
				directory.mkdirs();

				// commons-io, I miss you :(
				FileOutputStream fileOutputStream = new FileOutputStream(file);
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int len = 0;
				
				while (inputStream.available() > 0 && (len = inputStream.read(buffer)) != -1)
				{
					byteArrayOutputStream.write(buffer, 0, len);
				}
				
				fileOutputStream.write(byteArrayOutputStream.toByteArray());
				fileOutputStream.flush();
				
				result = getString(R.string.result) + file.getAbsolutePath();
			}
			catch (Exception ex)
			{
				Log.e(WebViewDemoActivity.class.toString(), ex.getMessage(), ex);
				result = ex.getClass().getSimpleName() + " " + ex.getMessage();
			}
			finally
			{
				if (inputStream != null)
				{
					try
					{
						inputStream.close();
					}
					catch (IOException ex)
					{
						Log.e(WebViewDemoActivity.class.toString(), ex.getMessage(), ex);
						result = ex.getClass().getSimpleName() + " " + ex.getMessage();
					}
				}
			}

			return result;
		}

		@Override
		protected void onPostExecute(String result)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(WebViewDemoActivity.this);
			builder.setMessage(result).setPositiveButton((R.string.ok), null).setTitle(getString(R.string.download));
			builder.show();

		}

	}

}
