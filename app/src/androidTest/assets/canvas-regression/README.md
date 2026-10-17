These two silent, two-second, solid-color H.264 fixtures are generated test data.
The Canvas regression samples actual decoded pixels before and after a URL change.
They use 256x256 baseline video because Qualcomm's hardware decoder rejects the
original 64x64 fixture. The test must exercise a supported video format on phones.

Generate each fixture with FFmpeg, replacing COLOR with red or blue:

```sh
ffmpeg -f lavfi -i color=c=COLOR:s=256x256:r=30:d=2 -an -c:v libx264 -threads 1 \
  -profile:v baseline -level:v 3.0 -pix_fmt yuv420p -colorspace bt709 \
  -color_primaries bt709 -color_trc bt709 -color_range tv -movflags +faststart COLOR.mp4
```
