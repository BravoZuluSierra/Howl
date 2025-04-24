# Howl

> [!CAUTION]
> **IMPORTANT DISCLAIMER** Howl is a free hobbyist app that carries no warranty, and we accept no liability. Any use of the app and any use of electronic stimulation ("estim") devices is at your own risk.

## Description

Howl is an Android app that controls the DG Lab Coyote 3, an estim device for erotic adult entertainment.

Current features are: -
* Playback of funscript files (normally used with "stroker" type toys), using an original algorithm.
* Playback of HWL files, which are preconverted audio files specifically for use with Howl.
* A wave generator with various different parameters and shapes.
* Several built-in "activities" to enjoy.

All features take advantage of the full capabilities of the Coyote 3, sending frequency and amplitude updates 40 times per second.

## Installation

Pre-built APK files have been provided under "Releases". You simply need to install the APK on your Android device.

## Device support

Howl is intended to support any Android version from 8.0 (released in 2017) onwards. However I've only personally tested it using Android 14.

It will only work on devices that support Bluetooth Low Energy (BLE), as this is required to connect to the Coyote.

## App screenshots

Here are some screenshots of different parts of the application.

### Player screenshot

![Player](/screenshots/player.png)

### Wave generator screenshot

![Wave generator](/screenshots/generator.png)

### Activity screenshot

![Activity](/screenshots/activity.png)

### Settings screenshot

![Coyote settings](/screenshots/settings.png)

## Electrode setup and balance

The suggested electrode setup is to connect your lower down electrode on channel "A" and your higher up electrode on channel "B". This is especially important for funscripts and some of the built-in activities which use positional effects.

Exactly what electrodes you're using or where to place them is personal preference. We just need "A" to be something you feel lower down, and "B" to be something you feel higher up.

We've provided some calibration patterns (found on the "Activity" tab) to help you choose optimal settings to get the best effect.

### Calibration 1 - positional effects

To optimally experience positional effects, it helps if the power levels to each of your electrodes are well balanced, with signals to A and B feeling equally strong.

Calibration 1 is a pattern that moves from electrode A to electrode B and back, repeating every 4 seconds. Just a single frequency (the middle of the configured range) is used. The goal is to adjust your main power levels for channel A and B so that each end (when the signal is fully on A or fully on B) feels equally strong to you.

Once you've done this adjustment, you should hopefully find that you can feel the illusion of sensation travelling through the middle (where you don't directly have electrodes) as it moves back and forth. This is the intended positional effect.

It's not an exact science as you'll probably want to adjust power levels over the course of a session. But the calibration should at least give you rough guide to think "Ah, I always need to set the channel A power higher to get a good balance with these electrodes" (or whatever you discovered, depending on your setup).

### Calibration 2 - frequency balance

Calibration 2 is a pattern that sweeps through all of the configured frequency range. It's designed to help you optimally set the frequency balance parameters (which are a feature of the Coyote hardware) so that high and low frequencies feel equally strong.

The pattern changes electrode periodically, as follows: -
* Electrode A only (16 seconds)
* Electrode B only (16 seconds)
* Both electrodes simultaneously (16 seconds)
* Then back to electrode A only, with the same pattern repeating forever.

Your goal is to make the lowest (most thumpy) frequency and the highest (most buzzy) frequency on each electrode feel equally strong to you. This can be a little tricky as they are different types of sensations, but this is all about your perception, so just give it your best shot.

You adjust this by changing "Channel A Frequency Balance" (or the equivalent for channel B), which is found on the "Settings" page.
* If the low frequencies are too weak and the highs feel stronger, increase the frequency balance.
* If the low frequencies are too strong and the highs feel weaker, reduce the frequency balance.

Take a note of the optimal frequency balance numbers that you discovered for your electrode configuration. As this is a Coyote feature, you can also reuse it in other places (e.g. the DG Labs app supports the same frequency balance setting).

Having a nice balance between the high and low frequencies helps give you a good experience across the whole frequency range. It also helps to limit "surprises" where a pattern might change frequency and suddenly feel much stronger.

## Main controls

Howl's main controls are displayed in the top section of the screen.

### Channel A/B power controls

The plus and minus buttons adjust the overall power on their corresponding channel up and down. This directly corresponds to the power level on the Coyote, and has a maximum range of 0 to 200. The maximum level you can turn the power up to is also governed by the power limit configured on the settings page (which is fairly conservatively set to 70 by default).

The step size for the power controls defaults to 1, but this can be increased on the settings page if you would like larger steps.

### Mute pulse output button

This button mutes all pulse output to the Coyote when activated, until it is pressed again. Files keep playing during this time, but no output is sent. This control is very handy if you want to briefly mute in order to adjust your electrodes. The fact that playback continues allows funscripts to remain in sync.

### Pulse chart toggle button

The chart toggle button (looks like a section of line graph) can show a basic pulse chart to give a visual display of what output Howl is generating. This is extremely useful for development and debugging, or just general interest. 

The button toggles between 3 chart modes: -

**Off (default)**
No chart is displayed, and there is no performance overhead.

**Combined**
A single chart is displayed, with time on the X axis (most recent on the right) and power on the Y axis. The top of the chart corresponds to maximum output power, and the bottom of the chart to zero power. The colour of each point denotes the channel and gives a rough guide to the frequency. Channel A points go between red (lowest frequency) and yellow (highest frequency). Channel B points go between blue (lowest frequency) and green (highest frequency).

**Split**
Two separate charts are displayed. For both charts the X axis is time (most recent on the right), red points are on channel A, and blue points are on channel B. The chart on the left shows power (maximum at the top and minimum at the bottom). The chart on the right shows frequency (maximum at the top and minimum at the bottom). The split chart can be harder to understand, but is useful if you need a more accurate display of what the frequency is doing.

Each point represents a set of values we've calculated to send to the Coyote, so points occur on each channel every 1/40th of a second to correspond to its maximum update rate. The last 200 pulses we've calculated (5 seconds of data) are shown by the charts.

The actual pulse output is smoother than the chart appears (for efficiency reasons we only update the chart 10 times per second). The chart cannot be used to diagnose performance issues as it only shows what we generated, not when or if the pulse was actually sent.

### Channel swap button

The channel swap button (with two opposing arrows) swaps the output on channel A and channel B when active. This is helpful if you connected your electrodes the wrong way round, or if you are playing an HWL file where the original audio had different electrode placement to Howl's preferred setup.

It does not swap the power controls, only what is output (i.e. channel A power always controls Coyote channel A regardless of this setting). The channel swap is also not reflected by the pulse charts, as it happens later in the pipeline.

### Frequency range slider

This slider sets the minimum and maximum frequency that Howl can use during playback. The full range of frequencies between these values may be used for output. Everything Howl plays is mapped in real-time to the range you've set. Don't forget that you can always adjust this control to your liking, whatever content happens to be playing.

Not every feature uses the entire frequency range you've set, but everything uses at least a subset of your configured range, so adjusting it will always do something.

## Player

The "Player" tab allows you to play back different kinds of files. Currently it supports funscripts and HWL files. Your files need to be stored somewhere on your device that doesn't require any special permissions to access (I just put mine in a subfolder inside "Documents").

### Player advanced options

This section explains the function of all the player options.

#### General settings

**Invert channel A/B frequencies**
This inverts all the frequencies played on that channel. So if it would have played the lowest frequency, it will instead play the highest frequency (and vice versa). Setting one or both of these when playing a converted audio file can be interesting, and sometimes gives a very different experience.

Be careful not to unintentionally leave this setting enabled, as it will affect all app output (features like the generator and activities also send their output via the player).

#### Funscript settings

**Volume (versus dynamic range)**
Increasing this makes funscript playback "louder" at a particular power level, but this is at the cost of dynamic range. The more you increase it, the less difference there will be in feeling between fast and slow actions (our algorithm generally gives faster movements higher power than slower ones). The default value is 0.5.

**Positional effect strength**
This adjusts the strength of the positional effect that Howl uses to "pan" between the electrodes when playing funscripts. At 0 there is no positional effect, and both electrodes always have the same power level. At 1 there is the maximum possible positional effect, with only channel B used for funscript positions at the very top, and only channel A used for positions at the very bottom.

The default value is 1. If you are only using a single channel setup, 0 will give you the best result, as the positional effect cannot work in a single channel configuration.

For some files or electrode setups, you might prefer mid-range values. For example at 0.7, the positional effect is pretty strong and you can clearly feel movement in line with the funscript, but there's still a bit of "spillover" onto the other electrode even for top/bottom positions.

**Feel adjustment**
This changes the way in which the configured frequency range is used, which allows some control over the general feeling of the script. The default value is 1.0. Values higher than 1 result in more use of lower frequencies, which can feel more "thumpy" and "physical". Values less than 1 result in more use of higher frequencies, which can feel more "buzzy" and "electrical". It's probably easiest to think of the control as shifting where the middle of the frequency range is (but everything is rescaled around this so the full range is still used).

Personally I've found higher values of this like 1.5 or 2.0 can be very nice with some scripts if you're looking for a more physical sensation.

**A/B frequency time offset**
The frequency we play is based on our calculation of where the "stroker" head of a funscript playback device would be (lower frequencies for bottom positions and higher frequencies for top positions). When this is set to 0, channels A and B both play exactly the same frequency, based on the current stroker position.

When this control is higher than 0, we instead take channel A's position (and resulting frequency) from the configured amount of time in the past. Channel B still uses the current time. This often results in us playing slightly different frequencies on both channels, which may give a more pleasing effect.

The default value is 0.1. Values in the range of 0.0 to 0.2 seem to give the best results, too large a time difference often just results in things feeling weird and out of sync.

This time offset only affects what frequency is used on channel A (it does not affect the positional effect, which is always based on the current time).

### Funscript playback tips

There is currently no support for syncing with any video player (this is something I hope to look at in future). You just need to have the .funscript files on your device, and manually press the play button at the same time as you start the video on whatever device you're using to view that.

Howl always tracks time exactly in real-time from when you pressed the play button, so as long as your player is also correct in this regard, it should not go out of sync during file playback. Use the "Mute pulse output" function if you need to make adjustments to your electrodes - this stops sending pulses, but the file keeps playing so it will remain in sync.

I found "action based" funscripts (where the motion of the stroker device corresponds to the physical actions in a video) gave the best experience. But this is by no means a requirement, any funscript should work.

Keep in mind that funscripts are originally for physical "stroker" devices, which cannot just jump instantly from the bottom to the top like an estim device can, and will have a maximum movement speed. So even a very well written script cannot always mirror a video perfectly, because the author will have been working within these limitations. That being said, there are also some really bad funscripts around that just aren't synced very well. So do try a few!

## Generator

The "Generator" feature is capable of playing back continuous waves with a number of different shapes and a wide range of parameters. These are generated mathematically, so should usually be as close to perfect waves as it's possible to represent on the Coyote.

The shapes, speeds and frequencies of the waves can all vary, so a vast number of different combinations is possible. You'll probably find some that are highly pleasurable, and some that aren't great. Every time you press the "Random" button, you'll get a different wave with a new set of parameters. Keep trying your luck, and you're almost guaranteed to find something you enjoy.

You can tap on the "Channel A" or "Channel B" parameter box if you'd like to manually configure the wave parameters yourself.

There's an "Automatically change parameters" option which is designed to be a provide a more continuous experience by changing the wave parameters over time. The concept is that it's a bit like somebody else playing around with the knobs and buttons on a control box for you. It's capable of adjusting parameters like speed, power and frequency smoothly over time at different rates. So the experience should evolve over time, in contrast to the random button which just immediately rerolls everything.

The probability of speed, power, frequency and shape changes can all be set independently when using the automatic mode. The maximum value of 1.0 here corresponds to an average of 10 changes per minute. So 0.1 would be 1 change per minute on average etc. But keep in mind these are just probabilities rather than guarantees, depending on chance you may see significantly more or less changes than the average.

## Activity

Howl offers several built-in "activities". These are small programs, usually with some random elements. They're intended to offer a more interesting alternative to having fixed preset patterns, or just playing back a file.

The activities are generally designed with the default 10Hz-100Hz frequency range in mind (but you can certainly change it and experiment). They do not necessarily use the full configured range, as each activity is intended to feel unique and noticeably different from the others.

A short description of the currently available activities is given below. Please keep in mind that nothing is gender restricted, regardless of how it's described. One of the great things about estim is that it's all just electrons, and your perception of patterns could be different from mine.

### Chaos
Generates completely random frequencies and amplitudes on both channels, the Coyote equivalent of "white noise". To add a bit of additional interest, how long each value is held for changes periodically and can be anywhere from 1-10 cycles (0.025 seconds to 0.25 seconds).

### Infinite licks
A phantom tongue (or is it multiple tongues?) repeatedly licks you at varying positions and speeds, with little regard for the laws of physics. It's a complicated pattern with instantaneous jumps and a high level of speed and amplitude variance. This is generally one for high frequency enjoyers, as only the top half of the configured frequency range is used.

### Luxury HJ
Inspired by some good old fashioned stroking, there's lots of smooth top to bottom activity using our positional algorithm. The speed shifts around smoothly and fairly often. "What makes it luxury?" you may ask. From time to time a different "bonus" pattern can appear on either electrode which is supposed to feel like flickering touches around that point directly. That might be a sweeping palm, or a probing finger. Depends on your imagination and where you put your electrodes I guess. Ooh la la!

### Milkmaster 3000
A merciless milking machine from the year 3000. This one can be pretty intense. Moo.

There are two very distinct stages. First the "WOMP" stage where strokes (which are randomly either top to bottom or bottom to top) continually increase in speed. So I think of it as going WOMP.... WOMP.... (getting faster and faster) WOMPWOMPWOMPWOMPWOMP! Then comes the BUZZ stage which continuously plays a low frequency on channel A and a high frequency on channel B (the exact frequencies vary randomly a bit and change over the buzz duration a bit). Then repeat.

### Opposites
Channel A's frequency and power both each shift around the whole range in fairly smooth curves at varying speeds. Channel B always does the exact opposite. So if A is at full power, B is at zero power. If A is at the highest frequency, B is at the lowest frequency. It's a simple idea, but I liked the result.

### Penetration
Inspired by: just sex innit mate. The channel B part of the wave is quite unusually shaped and is designed to peak at different points to the A wave, helping to represent the in/out movements. Lots of smooth speed changes over time. There are also some more subtle random changes to "feel" and how the frequency range is used over time. Uses plenty of artistic licence and needs a healthy dose of imagination (as with most of these).

### Sliding vibrator
Designed to feel like a vibrator being slowly and teasingly moved over... well whatever you put your electrodes on! Power is based on our positional algorithm and varies as the vibrator slowly moves around. Movement is smooth and at varying (but generally slow) speeds. Sometimes it will stop and hold position for a few seconds. The frequency sits on a fixed value for a while and only changes from time to time (representing the vibrator being switched to a different speed).

## Settings

### Coyote parameters

These are all the parameters that can be set on the Coyote itself. Functionality should be exactly the same as the equivalent settings in the DG Labs app.

**Channel A/B Power Limit**
Limits the device power on that channel to the selected level.

**Channel A/B Frequency Balance**
This controls the relative strength of high and low frequencies on that channel. Higher values give stronger low frequencies. The default is 160.

**Channel A/B Intensity Balance**
This seems to be another way to adjust the low frequencies on the relevant channel. I haven't found it very useful and tend to leave it at 0. It seems to mainly affect the very lowest supported frequencies, e.g. instead of playing at 1Hz it will actually send 10Hz if you increase the intensity balance a bit. I don't think it's particularly helpful for this app, since if you don't want the very lowest frequencies, you can just adjust the main frequency range control.

### Misc options

**Power control step size**
This sets how much the power level changes by when you press the large plus/minus buttons in Howl to change the channel A or B power. The default is 1. This is a convenience setting for users who like to use high power levels, allowing them to be reached without having to press the adjustment buttons as many times. The step size can be independently configured for each channel, which can be helpful when using two different kinds of electrode.

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

The app will ask for location permissions. Howl does not track your location at all, and this is simply because the location permission is required to scan for Bluetooth devices on most versions of Android (in theory it's not supposed to be needed after Android 12, but we found a lot of devices didn't follow Google's specification and still needed it, so for maximum compatibility we just always require the permission).

## License

All original code is released under the MIT License. There is an additional stipulation that if you distribute this app or any part of it (with or without changes), you may not use the name Howl, or any very similar name. This is to help make it clear to users that your distribution is not associated with this project.