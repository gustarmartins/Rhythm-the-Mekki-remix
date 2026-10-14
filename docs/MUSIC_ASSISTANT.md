# Music Assistant

Music Assistant is experimental and stays on the **6.1 Nightly** development
line. Stable 6.0.0 excludes it. The native catalog/control adapters are being
implemented separately; see [native integration status and feature comparison](MUSIC_ASSISTANT_NATIVE.md).
The screen described below still uses the server's web frontend.

Open **Settings → Rhythm GO → Music Assistant**, or search Settings for
**Music Assistant**, **Tailscale**, or **Sendspin**. It is also available on the
streaming Settings screen. Enabling Rhythm GO is not required to use it.

Enter the server's direct web URL, such as `http://raspberrypi:8095` or
`http://100.x.y.z:8095`, then sign in on the server page. A bare hostname defaults
to HTTP port 8095; an explicit HTTP/HTTPS URL keeps its port and proxy path.
Passwords and access tokens do not belong in the saved URL. Connect Tailscale on
the phone and server before using a tailnet address. HTTPS reverse proxies must
forward WebSocket connections. Home Assistant ingress URLs are not supported.

This integration embeds the Music Assistant server's own frontend. The server
provides browsing, search, queues, providers, player selection, queue transfer
and compatible player groups. It is not a native catalog provider in Rhythm GO.
To listen on the phone, enable Music Assistant's web player and select it in its
player menu. Current Music Assistant web players use
[Sendspin](https://www.music-assistant.io/player-support/sendspin/) for audio
and synchronization; use the server's group controls for supported combinations.
The server and its frontend determine available features and codec support.

Enable **Keep player active in background** before leaving the screen or locking
the phone. This keeps the web player in a foreground service with a partial wake
lock and a notification containing **Stop phone player**. Turn it off when done;
an enabled idle web player still holds the connection and wake lock. With the
option off, leaving the Activity destroys the phone player. Rotation preserves
the view through a retained application-context service binding. Other server
players are controlled by Music Assistant and are not stopped when this view closes.
There is no boot receiver, sticky restart, or saved background-enable preference.

Android 9 and later use a separate `:music_assistant` process and WebView data
directory. Main-process library repositories, artwork maintenance, and playback
services are not initialized in that process. Logins stay inside that WebView
profile, excluded from cloud backup and device transfer. **Forget server and
sign out** stops the phone player and clears this profile's cookies, web storage
and cache. No native JavaScript bridge or credentials injection is exposed.
Top-level navigation stays on the configured origin; external HTTP/HTTPS links
require an explicit browser action. Certificate errors are never bypassed.
On Android 8, opening the server uses the external browser because WebView data
directory isolation is unavailable.

Rhythm's native FIFO queue, library downloads, Bluetooth lyric formatting,
equalizer and JamesDSP session broadcasts belong to its native playback service.
Music Assistant audio uses the web player's own audio path. This integration
does not merge those queues or expose Music Assistant as a native Android Auto
library. Home Assistant/provider OAuth flows requiring other websites open in a
browser with a separate login session; use a Music Assistant account for the
embedded session. Music Assistant network access uses the device connection,
including cellular, independently of native Rhythm GO download/quality settings.

## Validation

`MusicAssistantServerTest` covers LAN/MagicDNS/IPv6 normalization, exact explicit
ports and proxy paths, credential/token rejection, origin boundaries, external
intent rejection and opt-in lifetime decisions. The Nightly workflow runs the
complete Github debug JVM suite, compiles both release flavors, runs release
lint/R8 and verifies APK versions and signatures before publication.

These checks do not establish real-server or physical playback acceptance. With
a configured server, verify login, library search, selecting this phone, playing
with the screen locked, rotation, stop notification, disconnect/reconnect over
Tailscale, forgetting and logging in again, queue transfer and a compatible
phone/PC/speaker group. Check that leaving remote-control mode does not stop
another player and that native Rhythm playback still follows its audio-focus
and Bluetooth behavior. No friend's server credentials are bundled or assumed.
