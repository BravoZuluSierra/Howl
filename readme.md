# Howl

> [!CAUTION]
> **IMPORTANT DISCLAIMER** Howl is a free hobbyist app that carries no warranty, and we accept no liability. Any use of the app and any use of electronic stimulation ("estim") devices is at your own risk.

## Description

Howl is an Android app that controls the DG Lab Coyote 3, an estim device for erotic adult entertainment.

Current features are: -
* A random wave generator with a wide range of a parameters
* Playback of HWL files, which are preconverted audio files specifically for use with Howl
* Playback of funscript files (normally used with "stroker" type toys), using an original algorithm

All features take advantage of the full capabilities of the Coyote 3, sending frequency and amplitude updates 40 times per second.

## Installation

Pre-built APK files have been provided under "Releases". You simply need to install the APK on your Android device.

## Device support

Howl is intended to support any Android version from 8.0 (released in 2017) onwards. However I've only personally tested it using Android 14.

It will only work on devices that support Bluetooth Low Energy (BLE), as this is required to connect to the Coyote.

## App screenshots

Here are some screenshots of different parts of the application.

### Player screenshot

![Player](https://i.imgur.com/0GDFWOj.png)

### Wave generator screenshot

![Wave generator](https://i.imgur.com/HQeUbLq.png)

### Coyote parameters screenshot

![Coyote settings](https://i.imgur.com/7qMl9SS.png)

## App functions and tips

Everything Howl can play (funscripts, HWL files, generated waves) is mapped in real-time to the range you've set using the app's main "Frequency range" slider control. Don't forget that you can always adjust this to your liking, whatever content happens to be playing.

### Player

The "Player" tab allows you to play back different kinds of files. Currently it supports funscripts and HWL files. Your files need to be stored somewhere on your device that doesn't require any special permissions to access (I've just been using a subfolder inside "Documents" when testing).

#### Electrode configuration for funscripts

Howl uses a positional algorithm when playing back funscript files. For a good experience, you need to be using both channels. Your lower electrode should be connected on channel "A" and your higher electrode should be connected on channel "B". Exactly what or where the electrodes are isn't really important, just that "A" needs to be something you feel lower down, and "B" needs to be something you feel higher up (if you connected them the wrong way round, you can simply toggle on the channel swap control, which is the one with two opposing arrows next to the mute button).

When the "stroker head" in a more traditional funscript setup would be at the top, Howl sends a stronger signal on channel B. When the head would be at the bottom, Howl sends a stronger signal on channel A. The idea is that you should feel movement from top to bottom as the "stroker" would move up and down, similar to how an audio panning effect works.

For this to work optimally, adjust the power on channel A and B so that you feel fairly equal power on both channels when the funscript is "pumping" up and down. You don't want one channel to be way stronger than the other, as this compromises the positional effect. You might need to experiment a bit to get this right, especially if you're using two different types of electrodes.

You also want the power across high and low frequencies to be fairly balanced, as you'll get more low frequencies sent to "A" and more high frequencies sent to "B" by default. You can use the "Frequency Balance" sliders in "Settings" for this. If the low frequencies are too strong, reduce the frequency balance. If the low frequencies are too weak, increase the frequency balance.

If you want to only use a single channel, you should set "Channel bias factor" to 0 in funscript settings. But be aware that this setup can't use the positional algorithm, so you are not getting the full experience.

#### Funscript playback tips

There is currently no support for syncing with any video player. You just need to have the .funscript files on your device, and manually press the play button at the same time as you start the video on whatever device you're using to view that.

Howl always tracks time exactly in real-time from when you pressed the play button, so as long as your player is also correct in this regard, it should not go out of sync during file playback. Use the "Mute pulse output" function if you need to make adjustments to your electrodes - this stops sending pulses, but the file keeps playing so it will remain in sync.

Generally "action based" funscripts (where the motion of the stroker device corresponds to the actions in a video) will give the best experience.

Keep in mind that funscripts are originally for physical "stroker" devices, which cannot just jump instantly from the bottom to the top like an estim device can, and will have a maximum movement speed. So even a very well written script cannot always mirror a video perfectly, because the author is working within these limitations.

#### Funscript advanced options

Here's what all the advanced funscript options do.

##### **Channel bias factor** (recommended values 0.6 to 0.9)
This adjusts the strength of the positional effect that Howl uses to "pan" between the electrodes. At 0, the positional effect is turned off, and both electrodes always have the same power level. At 1, only channel B will trigger for funscript positions at the very top, and only channel A will trigger for positions at the very bottom.

##### **Frequency separation factor** (recommended values 0.0 to 0.3)
This pushes the frequencies apart a little bit. Instead of playing the same frequency on both channels, channel A will be slightly lower, and channel B will be slightly higher, with how much depending on the value set. I'm not sure if this is actually particularly useful or not. I quite liked 0.1 and made that the default, but you might find setting 0 better.

##### **Invert channel A frequencies** (recommended off)
Normally the highest frequency is sent when the funscript stroker would be at the top, and the lowest frequency is sent when the funscript stroker would be at the bottom. This option reverses that for channel A, but not channel B, so instead of having similar frequencies to both channels, the channels do the reverse of each other as the stroker moves. This will probably feel less accurate to the script/video, but might well feel more pleasurable for some electrode setups, or if you are bored of the standard configuration and would like to get some high frequencies on channel A. If using this option I would suggest setting the separation factor to 0.

### Generator

The "Generator" tab is capable of playing back continuous waves of various types and with various parameters. These are generated mathematically, so should be as close to perfect waves as it's possible to represent on the Coyote.

The shapes, periods and frequencies of the waves are different every time. You'll probably find some that are highly pleasurable, and some that aren't great. Every time you press the "Random" button, you'll get a different wave with a new set of parameters. Keep trying your luck, and you're almost guaranteed to find something you enjoy.

You can tap on the "Channel A" or "Channel B" parameter box if you'd like to manually configure the wave parameters yourself.

There's an "Auto cycle" feature, which will automatically play a new random wave every time the configured number of seconds passes.

One more interesting thing is that you can put the power or frequency values in the opposite order to get different results. E.g. for most wave types setting power 20-70% would give you a noticably different shape to setting 70-20% (which generates the inverse of the wave).

### Settings

The settings pane edits all the parameters that can be set on the Coyote itself, so the functionality should be the same as the equivalent settings in the DG Labs app.

**Channel A/B Power Limit**
Limits the device power on that channel to the selected level.

**Channel A/B Frequency Balance**
This controls the relative strength of high and low frequencies on that channel. Higher values give stronger low frequencies. The default is 160.

**Channel A/B Intensity Balance**
This seems to be another way to adjust the low frequencies on the relevant channel. I haven't found it very useful and tend to leave it at 0. It seems to mainly affect the very lowest supported frequencies, e.g. instead of playing at 1Hz it will actually send 10Hz if you increase the intensity balance a bit. I don't think it's particularly helpful for this app, since if you don't want the very lowest frequencies, you can just adjust the main frequency range control.

## About HWL files and how to make them

### What's the point of HWL files?

HWL files are audio files that have been converted in advance to work with Howl. The reason for doing this is that estim audio files usually contain a lot of data (often 40,000+ samples per second per channel). The Coyote can only actually play 40 different pulses per second at most, and has a very limited frequency range compared to what an audio file can contain. So most of this audio data is never really used. Apps that play audio will usually do some computationally expensive frequency and amplitude detection on the audio file to decide what pulses to actually send to the Coyote.

I thought "What if we do all the complicated frequency detection stuff in advance instead of doing it on the user's phone?" So that's essentially what an HWL file is. It's just a representation of what 40 pulses per second the Coyote would actually play. But we've done all the calculations in advance instead of doing them as the file plays.

Advantages of this method: -
* Much smaller file size. E.g. one popular 40 minute estim audio mp3 file is 92MB in size. This was reduced to 1.5MB when converted to HWL format.
* Only very minimal processing is required on the user's device during playback, saving battery life.
* There is potential to use a more advanced and accurate frequency detection algorithm, as the frequency detection will be done on a powerful PC in advance and doesn't need to run on a phone.
* It's easier to map from the file to the Coyote's range, since we've already processed the whole audio in advance. So we already know what the loudest part of the file is, and what the highest frequency part of the file is, and can do scaling relative to that.

However there are some disadvantages, such as the format being specifically tailored to the 40 updates per second the Coyote 3 uses. That works well for this simple app, but wouldn't work for apps that want to support different devices with different capabilities. Another disadvantage is that if we change something about the converter (e.g. improving the algorithm, or fixing a bug) it's necessary to reconvert and redistribute existing HWL files to take advantage of those improvements.

### How to convert audio files to HWL format

HWL conversion is done using a Python script on a PC, which is checked in to this repository (the "hwl.py" script in the "Python" directory). The script has a couple of dependencies (NumPy for large array handling and Aubio, which we use for frequency detection). The process of getting it running under Windows is as follows: -

1) Download and install the latest Anaconda or Miniconda from [https://www.anaconda.com/download](https://www.anaconda.com/download) (if you're only using it for this get Miniconda as it's a smaller installation).
2) Make a folder wherever you want the HWL converter to live. Download the "hwl.py" script and put it in inside your new folder. Make another folder called "audio", also inside your new folder. It should look similar to the following.
![Converter folder](https://i.imgur.com/lKmucPG.png)
3) Launch "Anaconda Prompt" (under "Anaconda" on the start menu).
4) In the Anaconda prompt, run the command `conda create -n hwl python numpy aubio` This should create a new conda environment called "hwl" with all the dependencies that the converter script needs installed.

After installing, do the following whenever you want to run the converter script.
1) Put all the audio files you want to convert inside the "audio" folder you created earlier (this is where the script looks by default for audio files to convert). Currently it will convert .mp3 .flac and .wav files.
2) Launch "Anaconda Prompt" (under "Anaconda" on the start menu).
3) In the Anaconda prompt, run the command `activate hwl`
4) Run the command `cd "[path to the script folder you created earlier]"` e.g. `cd "c:\users\your_username\documents\hwl"` if you placed the script in a folder called "hwl" in your documents directory.
5) Run the command `python hwl.py`
6) You should see various output from the converter script as it runs and converts your audio files.

## Common questions and answers

**Can I use Howl with a different estim device?**
No, Howl is written specifically for the Coyote 3 and the Bluetooth protocol it uses.

**Can I use Howl on iOS?**
Unfortunately not. Howl is a native Android app, and it's not possible to port it to other platforms without writing most of the app again.

## Developer info

Howl is a native Android app, which is written in Kotlin and uses Jetpack Compose. The files in the repository are for Android Studio, which is the recommended way to work on or build the app.

### Technical description of the HWL format

The custom HWL format that the app uses for preconverted audio files is a very simple one.

The file starts with 8 byte header that always says "YEAHBOI!" (this is used by the app to identify if a file is an HWL file).
This header is followed by any number of pulse objects (one for every 1/40th second the file lasts, to correspond to the Coyote 3's maximum update rate).

Each pulse object consists of 4x IEEE 754 single precision floating point values in little endian format, in the following order: -
* left_channel_amplitude: 0.0 to 1.0
* right_channel_amplitude: 0.0 to 1.0
* left_channel_frequency: 0.0 to 1.0
* right_channel_frequency: 0.0 to 1.0

A 1.0 value would correspond to the loudest part of the file for amplitude, or the highest frequency part of the file for frequency. These values are all just relative and we don't store the original frequencies (these frequencies are mapped into the range the user has chosen on the frequency slider control when the HWL file is played back by the Howl app).

HWL files are always stereo - mono source files are lazily supported by just storing the same values for both channels.

## Privacy

Howl is a simple hobbyist app, and does not include any adverts or tracking. It does not send anything over the internet, and doesn't require internet access.

On Android versions before 12, the app will ask for location permissions. Howl does not track your location, and this is simply because the location permission is required to scan for Bluetooth devices on these versions. On later Android versions it should only ask for Bluetooth related permissions.

## License

All original code is released under the MIT License. There is an additional stipulation that if you distribute this app or any part of it (with or without changes), you may not use the name Howl, or any very similar name. This is to help make it clear to users that your distribution is not associated with this project.