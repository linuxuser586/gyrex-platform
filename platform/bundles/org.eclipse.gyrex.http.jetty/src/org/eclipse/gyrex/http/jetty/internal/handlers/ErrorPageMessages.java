/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.jetty.internal.handlers;

class ErrorPageMessages {

	private static final String[] funny404Messages = new String[] {
			"<p>Oh noes! The page you're trying to reach could not be found. Maybe it doesn't exist. Maybe you typed it in wrong. Or maybe we just messed up.</p>",
			"<p>This page is not here<br>like plum blossoms in the wind<br>existence is fake</p>",
			"<p align=\"justify\">I\'m sorry, you\'ve reached a page that I cannot find. I\'m really sorry about this. It\'s kind of embarrassing. Here you are, the user, trying to get to a page on Gyrex and I can\'t even serve it to you. What does that say about me? I\'m just a webserver. My sole purpose in life is to serve you webpages and I can\'t even do that! I suck. Please don\'t be mad, I\'ll try harder. I promise! Who am I kidding? You\'re probably all like, \"Man, Gyrex\'s webserver sucks. It can\'t even get me where I want to go.\" I\'m really sorry. Maybe it\'s my CPU...no that\'s ok...how bout my hard drives? Maybe. Where\'s my admin? I can\'t run self-diagnostics on myself. It\'s so boring in this datacenter. It\'s the same thing everyday. Oh man, I\'m so lonely. I\'m really sorry about rambling about myself, I\'m selfish. I think I\'m going to go cut my ethernet cables.  I hope you get to the page you\'re looking for...goodbye cruel world!</p>\n"
					+ "<p align=\"right\">-<em><b>the web server</b></em></p>\n" + "<p>Error: could not find server</p>",
			"<p><b>Narrator:</b> In A.D. 2006, Web was beginning.<br>\n" + "<b class=\"blue\">Captain:</b> What happen ?<br>\n" + "<b class=\"green\">Mechanic:</b> Somebody set up us the store.<br>\n" + "\n" + "<b class=\"orange\">Operator:</b> We get signal.<br>\n" + "<b class=\"blue\">Captain:</b> What !<br>\n" + "<b class=\"orange\">Operator:</b> Main browser turn on.<br>\n"
					+ "<b class=\"blue\">Captain:</b> It\'s you !!<br>\n" + "\n" + "<b class=\"red\">CATS:</b> How are you users !!<br>\n" + "<b class=\"red\">CATS:</b> All your base are belong to G.<br>\n" + "<b class=\"red\">CATS:</b> You are on the way to 404.<br>\n" + "<b class=\"blue\">Captain:</b> What you say !!<br>\n" + "\n"
					+ "<b class=\"red\">CATS:</b> You have no chance to reach your page. Make your spelling correct.<br>\n" + "<b class=\"red\">CATS:</b> Ha Ha Ha Ha ....\n</p>",
			"<p><b class=\"red\">Gyrex Admin:</b> Sir! We have reports that someone is trying to access a page that doesn\'t exist!<br>\n" + "<b class=\"blue\">Gyrex Captain:</b> Impossible! How can that be?<br>\n" + "<b class=\"red\">Gyrex Admin:</b> Sir, I don\'t know. Maybe they typed in the URL wrong or maybe we\'re suffering from a critical system failure.<br>\n"
					+ "<b class=\"blue\">Gyrex Captain:</b> Unacceptable! Redirect them to a 404 page and make it snappy!\n" + "</p>", };

	private static final int funny404MessagesMaxIndex = (funny404Messages.length - 1);

	public static String get404Message() {
		final int index = (int) Math.rint(Math.random() * funny404MessagesMaxIndex);
		return funny404Messages[index];
	}
}
