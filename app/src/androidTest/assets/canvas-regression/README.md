These two silent, one-second, solid-color H.264 fixtures are generated test data.
The Canvas regression samples actual decoded pixels before and after a URL change.

Generate each fixture with FFmpeg, replacing COLOR with red or blue:

```sh
ffmpeg -f lavfi -i color=c=COLOR:s=64x64:r=10:d=1 -an -c:v libx264 -threads 1 \
  -pix_fmt yuv420p -movflags +faststart COLOR.mp4
```
