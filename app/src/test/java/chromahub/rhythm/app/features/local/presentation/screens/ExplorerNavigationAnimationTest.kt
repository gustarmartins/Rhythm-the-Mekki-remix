/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.features.local.presentation.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ExplorerNavigationAnimationTest {

    @Test
    fun `resolveFolderNavigationDirection returns forward when entering a subfolder from root`() {
        assertEquals(
            FOLDER_NAVIGATION_FORWARD,
            resolveFolderNavigationDirection(
                initialPath = null,
                targetPath = "/storage/emulated/0"
            )
        )
    }

    @Test
    fun `resolveFolderNavigationDirection returns forward when entering a deeper subfolder`() {
        assertEquals(
            FOLDER_NAVIGATION_FORWARD,
            resolveFolderNavigationDirection(
                initialPath = "/storage/emulated/0/Music",
                targetPath = "/storage/emulated/0/Music/Rock"
            )
        )
    }

    @Test
    fun `resolveFolderNavigationDirection returns backward when returning to the parent folder`() {
        assertEquals(
            FOLDER_NAVIGATION_BACKWARD,
            resolveFolderNavigationDirection(
                initialPath = "/storage/emulated/0/Music/Rock",
                targetPath = "/storage/emulated/0/Music"
            )
        )
    }

    @Test
    fun `resolveFolderNavigationDirection returns backward when returning to root`() {
        assertEquals(
            FOLDER_NAVIGATION_BACKWARD,
            resolveFolderNavigationDirection(
                initialPath = "/storage/emulated/0/Music",
                targetPath = null
            )
        )
    }

    @Test
    fun `resolveFolderNavigationDirection defaults to forward for unrelated transitions`() {
        assertEquals(
            FOLDER_NAVIGATION_FORWARD,
            resolveFolderNavigationDirection(
                initialPath = "/storage/emulated/0/Music/Rock",
                targetPath = "/storage/emulated/0/Music/Jazz"
            )
        )
    }

    @Test
    fun `resolveFolderNavigationDirection returns backward when jumping multiple levels up via breadcrumbs`() {
        assertEquals(
            FOLDER_NAVIGATION_BACKWARD,
            resolveFolderNavigationDirection(
                initialPath = "/storage/emulated/0/Music/Rock/Classic/70s",
                targetPath = "/storage/emulated/0/Music"
            )
        )
    }
}
