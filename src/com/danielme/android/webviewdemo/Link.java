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

import android.graphics.Bitmap;

public class Link
{
	
	private String url;
	
	private Bitmap favicon;
	
	public Link()
	{
		
	}

	
	public Link(String url, Bitmap favicon)
	{
		super();
		this.url = url;
		this.favicon = favicon;
	}


	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public Bitmap getFavicon()
	{
		return favicon;
	}

	public void setFavicon(Bitmap favicon)
	{
		this.favicon = favicon;
	}
	
	

}
