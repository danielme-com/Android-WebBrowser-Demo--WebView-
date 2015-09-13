/*
 * Copyright (C) 2012 Daniel Medina <http://danielme.com>
 * 
 * This file is part of "Android WebView Demo".
 * 
 * "Android WebView Demo" is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * "Android WebView Demo" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 3
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-3.0.html/>
 */

package com.danielme.android.webviewdemo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebIconDatabase;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings.PluginState;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class WebViewDemoActivity extends Activity
{
	private WebView webview;

	private ProgressBar progressBar;

	private EditText urlEditText;

	private List<Link> historyStack;

	private ArrayAdapter<Link> dialogArrayAdapter;

	private Button stopButton;
	
	private ImageView faviconImageView;
	
	private static final Pattern urlPattern = Pattern.compile("^(https?|ftp|file)://(.*?)");


	@SuppressLint({ "SetJavaScriptEnabled", "NewApi" })
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		historyStack = new LinkedList<Link>();
		webview = (WebView) findViewById(R.id.webkit);
		faviconImageView = (ImageView) findViewById(R.id.favicon);

		urlEditText = (EditText) findViewById(R.id.url);
		progressBar = (ProgressBar) findViewById(R.id.progressbar);	
		stopButton = ((Button) findViewById(R.id.stopButton));
		//favicon, deprecated since Android 4.3 but it's still necesary O_O ¿?
		WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());		
		
		// javascript and zoom
		webview.getSettings().setJavaScriptEnabled(true);
		webview.getSettings().setBuiltInZoomControls(true);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
		{
			webview.getSettings().setPluginState(PluginState.ON);
		}
		else
		{
			//IMPORTANT!! this method is no longer available since Android 4.3
			//so the code doesn't compile anymore
			webview.getSettings().setPluginsEnabled(true);
		}

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
				for(Link link : historyStack)
				{
					if (link.getUrl().equals(WebViewDemoActivity.this.webview.getUrl()))
					{
						link.setTitle(title);
					}
				}
			}
			
			@Override
			public void onReceivedIcon(WebView view, Bitmap icon)
			{	 			
				faviconImageView.setImageBitmap(icon);
				view.getUrl();
				boolean b = false;
				ListIterator<Link> listIterator = historyStack.listIterator();
				while (!b && listIterator.hasNext())
				{
					Link link = listIterator.next();
					if (link.getUrl().equals(view.getUrl()))
					{
						link.setFavicon(icon);
						b = true;
					}
				}
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
		
		dialogArrayAdapter = new ArrayAdapter<Link>(this, R.layout.history, historyStack)
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				//holder pattern
				LinkHolder holder = null;
				if (convertView == null)
				{
					LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					convertView = inflater.inflate(R.layout.history, null);
					holder = new LinkHolder();
					holder.setUrl((TextView) convertView.findViewById(R.id.textView1));
					holder.setImageView((ImageView) convertView.findViewById(R.id.favicon));

					convertView.setTag(holder);
				}
				else
				{
					holder = (LinkHolder) convertView.getTag();
				}

				Link link = historyStack.get(position);
				//show title when available
				if (link.getTitle() != null)
				{
					holder.getUrl().setText(link.getTitle());
				}
				else
				{
					holder.getUrl().setText(link.getUrl());
				}
				Bitmap favicon = link.getFavicon();
				if (favicon == null)
				{
					holder.getImageView().setImageDrawable(super.getContext().getResources().getDrawable(R.drawable.favicon_default));
				}
				else
				{
					holder.getImageView().setImageBitmap(favicon);
				}

				return convertView;
			}
		};

		builder.setAdapter(dialogArrayAdapter, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int item)
			{
				webview.loadUrl(historyStack.get(item).getUrl());
				stopButton.setEnabled(true);
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
			if (checkConnectivity())
			{
				//resets favicon
				WebViewDemoActivity.this.faviconImageView.setImageDrawable(WebViewDemoActivity.this.getResources().getDrawable(R.drawable.favicon_default));
				// shows the current url
				WebViewDemoActivity.this.urlEditText.setText(url);
				
				//only one occurrence
				boolean b = false;
				ListIterator<Link> listIterator = historyStack.listIterator();
				while (listIterator.hasNext() && !b)
				{
					if (listIterator.next().getUrl().equals(url))
					{
						b = true;
						listIterator.remove();
					}
				}
				Link link = new Link(url, favicon);
				historyStack.add(0, link);
	
				stopButton.setEnabled(true);
				updateButtons();			
			}
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{
			stopButton.setEnabled(false);
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
		inputMethodManager.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);

		if (checkConnectivity())
		{	
			stopButton.setEnabled(true);		
			
			//http protocol by default
			if (!urlPattern.matcher(urlEditText.getText().toString()).matches())
			{
				 urlEditText.setText("http://" + urlEditText.getText().toString());
			}
			webview.loadUrl(urlEditText.getText().toString());
		}
	}

	public void back(View view)
	{
		if (checkConnectivity())
		{
			webview.goBack();
		}
	}

	public void forward(View view)
	{
		if (checkConnectivity())
		{
			webview.goForward();
		}
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
			String urlString = arg0[0];
			
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			{	
				InputStream inputStream = null;
				FileOutputStream fileOutputStream = null;
				try
				{		
					
					URL url = new URL(urlString);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setRequestMethod("GET");
					inputStream = connection.getInputStream();
					
					String fileName = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/webviewdemo";
					File directory = new File(fileName);
					File file = new File(directory, urlString.substring(urlString.lastIndexOf("/")));				
					directory.mkdirs();
	
					// commons-io, I miss you :(
					fileOutputStream = new FileOutputStream(file);
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
					if (fileOutputStream != null)
					{
						try
						{
							fileOutputStream.close();
						}
						catch (IOException ex)
						{
							Log.e(WebViewDemoActivity.class.toString(), ex.getMessage(), ex);
							result = ex.getClass().getSimpleName() + " " + ex.getMessage();
						}
					}
				}
			}
			else
			{
				result = getString(R.string.nosd);
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
	
	/**
	 * Checks networking status.
	 */
	private boolean checkConnectivity()
	{
		boolean enabled = true;

		ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connectivityManager.getActiveNetworkInfo();
		
		if ((info == null || !info.isConnected() || !info.isAvailable()))
		{
			enabled = false;
			Builder builder = new Builder(this);
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setMessage(getString(R.string.noconnection));
			builder.setCancelable(false);
			builder.setNeutralButton(R.string.ok, null);
			builder.setTitle(getString(R.string.error));
			builder.create().show();		
		}
		return enabled;			
	}

}
