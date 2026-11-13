---
layout: default
title: Privacy Policy
---

# Privacy Policy

_Last updated: 2026-11-13_

Plain Light is an Android app for switching and dimming Bluetooth ceiling lights whose own app is
only in Chinese. This policy describes what the app does with the information you put into it. It is
short because the app does very little.

## The app collects nothing

Plain Light has no account, no sign-in, and no server. The app does not have the internet
permission, so nothing you enter is transmitted anywhere.

Everything the app keeps (your lights, their names, scenes and settings) is stored **only in the
app's private storage on your own device**, and is readable only by the app.

There is no analytics, no crash reporting, no advertising, and no third-party SDK that phones home.
The Google Play Billing library, which handles tips, carries usage logging of its own; without the
`INTERNET` permission it has no way to send it.

## What the app can access, and why

- **Nearby devices (Bluetooth).** Lets the app send commands to your lights. Your lights only
  listen, so the app broadcasts short Bluetooth signals and never scans for, connects to or reads
  from any device. Like the light's own remote, those signals can be picked up by any Bluetooth
  device in range; they carry only the command and a pairing number the app made up at random,
  nothing about you. It does not use your location. On Android 11 and older this is granted with the
  app rather than asked for.
- **Google Play billing.** Lets the app ask the Google Play app for the price of a tip and open its
  payment sheet. It gives the app nothing about you.

## The three things that happen outside the app

These are done by the operating system, by Google or by you, not by Plain Light, but they are the
honest answer to "where could my data be":

1. **Android's automatic backup.** If it is enabled on your phone, Android backs up the app's data
   to your own Google Drive, in a space that does not count against your storage quota and that
   Google states apps cannot read. It covers everything the app stores, your settings included, so
   they come back when you set up a new phone. You can turn this off in your phone's settings.
2. **Google Play.** Installing and updating an app is a transaction between your phone and Google,
   with its own privacy policy. About installs, the developer of this app receives aggregate,
   anonymous statistics — install counts, crash rates, country-level breakdowns — and no information
   about individuals.

   **A tip, if you leave one, is paid to Google Play.** The app asks the Play app on your phone for
   the prices and opens Play's own payment sheet; your payment details go to Google and never to the
   app. Tips unlock nothing, so the app keeps no record of one. The developer receives the payment
   and Google's order reports, which show for each tip the product, the price, your country, state
   and postcode, and the model of your phone — and your city, in the few countries where the developer
   rather than Google is the seller — but not your name or payment details. The developer uses them
   only for accounting.
3. **An email you choose to send.** *Report a problem*, *Suggest a feature* and *Something read
   wrong?* open your own email app with a draft addressed to the developer. The draft fills in the
   app version, Android version, phone model and language, and nothing you have kept in the app.
   You can read and change every line, and nothing is sent unless you send it from your email app.
   The developer uses what you send only to answer you and fix the app.

## Children

The app is not directed at children and collects nothing from anyone.

## Deleting your data

Uninstalling the app removes everything it stored on your phone. The only other copy is the one in
Android's backup, if you have it on, and that is kept and deleted by Android under your Google
account's backup settings. The app itself sends nothing anywhere else.

## Changes

If this policy changes, the date at the top changes with it, and the history of this file is public
in the repository.

## Contact

**plain-light-control.support@maksforge.com**
