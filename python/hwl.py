import os
import numpy as np
import struct
import itertools
from pathlib import Path
from aubio import source, pitch

def percentile_min_max(array1, array2):
    """
    Return the 0.5 percentile value and 99.5 percentile values from a numpy array
    Used instead of min/max to avoid selecting outlier or erroneously detected frequencies
    """
    array1_min = np.percentile(array1, 0.5)
    array2_min = np.percentile(array2, 0.5)
    array1_max = np.percentile(array1, 99.5)
    array2_max = np.percentile(array2, 99.5)

    combined_min = np.minimum(array1_min, array2_min)
    combined_max = np.maximum(array1_max, array2_max)
    return combined_min, combined_max

def normalise_array(array, min_val, max_val):
    """
    Normalise a numpy array between 0 and 1 based on min_val and max_val
    clamping values outside that range    
    """
    clamped_array = np.clip(array, min_val, max_val)
    normalised_array = (clamped_array - min_val) / (max_val - min_val)
    return normalised_array

def get_audio_files(folder_path):
    """
    Return a list of all the supported audio files below a directory (recursive)
    """
    folder = Path(folder_path)
    extensions = ["*.mp3", "*.wav", "*.flac"]
    audio_files = list(
        itertools.chain.from_iterable(
            folder.rglob(pattern) for pattern in extensions
        )
    )
    return audio_files

def write_output_file(destination_filename, left_amplitudes, right_amplitudes, left_frequencies, right_frequencies):
    """
    Write our output HWL file
    An HWL file simply consists of an 8 byte header that always says "YEAHBOI!"
    followed by any number of pulse objects (one for every 1/40th second the file lasts)

    Each pulse object consists of 4x IEEE 754 single precision floating point values in little endian format, as follows: -
    left_channel_amplitude: 0.0 to 1.0
    right_channel_amplitude: 0.0 to 1.0
    left_channel_frequency: 0.0 to 1.0
    right_channel_frequency: 0.0 to 1.0
    """
    if len(left_amplitudes) != len(right_amplitudes) or len(left_amplitudes) != len(left_frequencies) or len(left_amplitudes) != len(right_frequencies):
        raise ValueError("All output arrays must be the same length.")
    if len(left_amplitudes) == 0:
        raise ValueError("Output arrays must contain data.")
    with open(destination_filename, 'wb') as file:
        header = "YEAHBOI!"
        header_bytes = header.encode('utf-8')
        file.write(header_bytes)
        for left_amp, right_amp, left_freq, right_freq in zip(left_amplitudes, right_amplitudes, left_frequencies, right_frequencies):
            file.write(struct.pack('<ffff', left_amp, right_amp, left_freq, right_freq))

audio_directory = "audio"
desired_interval = 0.025 #1/40 of a second
window_size = 4096
hop_size = 128
samplerate = 44100
max_freq_lower_limit = 800.0

print(f"Sample rate={samplerate}, Window size={window_size}, Hop size={hop_size}")

audio_files = get_audio_files(audio_directory)
print("Files to be processed:")
print(audio_files)
for audio_file in audio_files:
    print(f"\nProcessing {audio_file.name}")
    destination_filename = audio_file.with_suffix('.hwl')
    if destination_filename.exists():
        print(f"Converted file already exists, skipping.")
        continue
    src = source(str(audio_file), samplerate, hop_size, channels=2)
    print(f"Samplerate={src.samplerate}, Channels={src.channels}, Duration={src.duration}")
    
    pitch_detector_left = pitch("default", window_size, hop_size, samplerate)
    pitch_detector_left.set_unit("Hz")
    pitch_detector_right = pitch("default", window_size, hop_size, samplerate)
    pitch_detector_right.set_unit("Hz")
    
    binned_frequencies_left = []
    binned_frequencies_right = []
    binned_amplitudes_left = []
    binned_amplitudes_right = []
    current_frequencies_left = []
    current_frequencies_right = []
    current_amplitudes_left = []
    current_amplitudes_right = []

    total_frames = 0
    current_bin_start_time = 0

    print("Detecting frequencies (this may take some time for large files)")
    try:
        for frames in src:
            if(len(frames[0]) < hop_size):
                break
            left_pitch = pitch_detector_left(frames[0])[0]
            right_pitch = pitch_detector_right(frames[1])[0]
            # left_confidence = pitch_detector_left.get_confidence()
            # right_confidence = pitch_detector_right.get_confidence()
            left_amplitude = np.sum(frames[0]**2) / len(frames[0])
            right_amplitude = np.sum(frames[1]**2) / len(frames[1])
            current_time = total_frames / float(samplerate)
            if current_time < current_bin_start_time + desired_interval:
                current_frequencies_left.append(left_pitch)
                current_frequencies_right.append(right_pitch)
                current_amplitudes_left.append(left_amplitude)
                current_amplitudes_right.append(right_amplitude)
            else:
                # the pitch detector produces values every hop, so we'll get more values than we want
                # "bin" them as we go along so that we just generate one value every desired_interval
                binned_frequencies_left.append(np.median(current_frequencies_left))
                binned_frequencies_right.append(np.median(current_frequencies_right))
                binned_amplitudes_left.append(np.mean(current_amplitudes_left))
                binned_amplitudes_right.append(np.mean(current_amplitudes_right))
                
                current_frequencies_left = [left_pitch]
                current_frequencies_right = [right_pitch]
                current_amplitudes_left = [left_amplitude]
                current_amplitudes_right = [right_amplitude]
                current_bin_start_time += desired_interval

            total_frames += frames.shape[-1]
        if current_frequencies_left:
            binned_frequencies_left.append(np.median(current_frequencies_left))
            binned_frequencies_right.append(np.median(current_frequencies_right))
            binned_amplitudes_left.append(np.mean(current_amplitudes_left))
            binned_amplitudes_right.append(np.mean(current_amplitudes_right))
    except:
        continue
    
    #print(f"Binned lengths {len(binned_frequencies_left)} {len(binned_frequencies_right)} {len(binned_amplitudes_left)} {len(binned_amplitudes_right)}")
    if len(binned_frequencies_left)==0:
        continue

    print("Normalising data")
    min_amp, max_amp = percentile_min_max(binned_amplitudes_left, binned_amplitudes_right)
    min_freq, max_freq = percentile_min_max(binned_frequencies_left, binned_frequencies_right)
    print(f"Detected min_amp={min_amp:.3f}, max_amp={max_amp:.3f}, min_freq={min_freq:.3f}Hz, max_freq={max_freq:.3f}Hz")
    if max_freq < max_freq_lower_limit:
        max_freq = max_freq_lower_limit
    print(f"Normalising amplitudes using 0-{max_amp:.3f} range, frequencies using 0-{max_freq:.3f}Hz range.")
    left_amplitudes = normalise_array(binned_amplitudes_left, 0, max_amp)
    right_amplitudes = normalise_array(binned_amplitudes_right, 0, max_amp)
    left_frequencies = normalise_array(binned_frequencies_left, 0, max_freq)
    right_frequencies = normalise_array(binned_frequencies_right, 0, max_freq)
    
    print(f"Writing output file {destination_filename}")
    write_output_file(destination_filename, left_amplitudes, right_amplitudes, left_frequencies, right_frequencies)
