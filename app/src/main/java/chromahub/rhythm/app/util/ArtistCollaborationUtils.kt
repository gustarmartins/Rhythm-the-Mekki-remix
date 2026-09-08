/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.data.model.Album

/**
 * Utility object for handling artist collaborations and separating individual artists
 * from collaboration strings consistently across the app.
 */
object ArtistCollaborationUtils {
    
    /**
     * List of separators commonly used in artist collaboration strings
     */
    private val COLLABORATION_SEPARATORS = listOf(
        ", ", ",", " & ", " and ", "&", " feat. ", " featuring ", " ft. ", " f. ",
        " with ", " + ", " vs ", " VS ", " / ", ";", " · ", " - ",
        " presents ", " pres. ", " and friends", " & friends", " × "
    )
    
    /**
     * Common non-artist strings that should be filtered out
     */
    private val NON_ARTIST_PATTERNS = setOf(
        "various", "va", "unknown", "compilation", "soundtrack", "ost"
    )
    
    /**
     * Splits an artist string into individual artist names, handling collaborations properly.
     * 
     * @param artistString The original artist string (may contain collaborations)
     * @return List of individual artist names
     */
    fun splitArtistString(artistString: String): List<String> {
        var result = artistString.trim()
        
        // Remove all content in parentheses and brackets, as it often contains metadata not part of the core artist name
        result = result.replace(Regex("\\s*\\([^)]*\\)", RegexOption.IGNORE_CASE), "")
        result = result.replace(Regex("\\s*\\[[^\\]]*\\]", RegexOption.IGNORE_CASE), "")
        
        // Replace all separators with a consistent delimiter
        COLLABORATION_SEPARATORS.forEach { separator ->
            result = result.replace(separator, "||", ignoreCase = true)
        }
        
        return result.split("||")
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 1 }
            .filter { name ->
                // Filter out common non-artist strings
                !NON_ARTIST_PATTERNS.contains(name.lowercase())
            }
            .distinct() // Remove any duplicates that might arise from processing
    }
    
    /**
     * Normalizes an artist name for comparison purposes.
     * 
     * @param artistName The artist name to normalize
     * @return Normalized artist name
     */
    fun normalizeArtistName(artistName: String): String {
        return artistName.trim().lowercase()
    }
    
    /**
     * Checks if a given artist name matches any of the artists in a collaboration string.
     * 
     * @param targetArtist The artist name to look for
     * @param collaborationString The collaboration string to search in
     * @return True if the artist is found in the collaboration
     */
    fun isArtistInCollaboration(targetArtist: String, collaborationString: String): Boolean {
        val normalizedTarget = normalizeArtistName(targetArtist)
        val collaborators = splitArtistString(collaborationString)
            .map { normalizeArtistName(it) }
        
        return collaborators.any { collaborator ->
            collaborator == normalizedTarget || 
            normalizedTarget.contains(collaborator) || 
            collaborator.contains(normalizedTarget)
        }
    }
    
    /**
     * Filters songs that belong to a specific artist, considering collaborations.
     * 
     * @param songs List of all songs
     * @param artistName The artist to filter by
     * @return List of songs that belong to the artist
     */
    fun filterSongsByArtist(songs: List<Song>, artistName: String): List<Song> {
        val normalizedArtistName = normalizeArtistName(artistName)
        
        return songs.filter { song ->
            val normalizedSongArtist = normalizeArtistName(song.artist)
            
            // Exact match
            if (normalizedSongArtist == normalizedArtistName) {
                return@filter true
            }
            
            // Check if artist is in collaboration
            isArtistInCollaboration(artistName, song.artist)
        }
    }
    
    /**
     * Filters albums that belong to a specific artist, considering collaborations.
     * 
     * @param albums List of all albums
     * @param artistName The artist to filter by
     * @return List of albums that belong to the artist
     */
    fun filterAlbumsByArtist(albums: List<Album>, artistName: String): List<Album> {
        val normalizedArtistName = normalizeArtistName(artistName)
        
        return albums.filter { album ->
            val normalizedAlbumArtist = normalizeArtistName(album.artist)
            
            // Exact match
            if (normalizedAlbumArtist == normalizedArtistName) {
                return@filter true
            }
            
            // Check if artist is in collaboration
            isArtistInCollaboration(artistName, album.artist)
        }
    }
    
    /**
     * Creates individual Artist objects from collaborations found in songs.
     * 
     * @param existingArtists List of existing Artist objects
     * @param songs List of all songs to extract collaborations from
     * @return List of individual Artist objects including extracted ones
     */
    fun extractIndividualArtists(existingArtists: List<Artist>, songs: List<Song>): List<Artist> {
        val processedArtists = mutableListOf<Artist>()
        val artistToSongs = mutableMapOf<String, MutableList<Song>>()
        val artistToAlbums = mutableMapOf<String, MutableSet<String>>()
        
        // Process all songs to extract individual artists
        songs.forEach { song ->
            val individualArtists = splitArtistString(song.artist)
            individualArtists.forEach { artistName ->
                val normalizedName = normalizeArtistName(artistName)
                if (normalizedName.isNotBlank() && normalizedName.length > 1) {
                    // Add song to this artist
                    artistToSongs.getOrPut(artistName) { mutableListOf() }.add(song)
                    
                    // Add album to this artist (if song has album info)
                    if (song.album.isNotBlank()) {
                        artistToAlbums.getOrPut(artistName) { mutableSetOf() }.add(song.album)
                    }
                }
            }
        }
        
        // First, add existing artists that have individual names (not collaborations)
        existingArtists.forEach { existingArtist ->
            val splitNames = splitArtistString(existingArtist.name)
            if (splitNames.size == 1) {
                // This is an individual artist, keep it
                processedArtists.add(existingArtist)
                // Remove from our extracted list to avoid duplicates
                artistToSongs.remove(existingArtist.name)
                artistToAlbums.remove(existingArtist.name)
            }
        }
        
        // Now create new Artist objects for individual artists extracted from collaborations
        artistToSongs.forEach { (artistName, artistSongs) ->
            // Check if we already have this artist (case-insensitive)
            val existingArtist = processedArtists.find { 
                normalizeArtistName(it.name) == normalizeArtistName(artistName)
            }
            
            if (existingArtist == null) {
                // Create a new Artist object for this individual artist
                val artistAlbums = artistToAlbums[artistName] ?: emptySet()
                val newArtist = Artist(
                    id = "extracted_${artistName.hashCode()}", // Generate a unique ID
                    name = artistName,
                    numberOfTracks = artistSongs.size,
                    numberOfAlbums = artistAlbums.size,
                    artworkUri = null // Don't use album artwork for artists - let repository fetch proper artist images
                )
                processedArtists.add(newArtist)
            } else {
                // Update existing artist's counts if they're higher
                val artistAlbums = artistToAlbums[artistName] ?: emptySet()
                val updatedArtist = existingArtist.copy(
                    numberOfTracks = maxOf(existingArtist.numberOfTracks, artistSongs.size),
                    numberOfAlbums = maxOf(existingArtist.numberOfAlbums, artistAlbums.size)
                )
                val index = processedArtists.indexOf(existingArtist)
                processedArtists[index] = updatedArtist
            }
        }
        
        return processedArtists
    }
}
