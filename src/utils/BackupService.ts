/**
 * EasyKiosk - BackupService
 * Handles backup and restore of app configuration
 */

import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform, PermissionsAndroid } from 'react-native';
import RNFS from 'react-native-fs';
import UpdateModule from './UpdateModule';
import { hasSecurePin } from './secureStorage';

// All storage keys to backup
const BACKUP_KEYS = [
  '@kiosk_url',
  '@kiosk_auto_reload',
  '@kiosk_hide_error_url',
  '@kiosk_always_load_from_network',
  '@kiosk_enabled',
  '@kiosk_auto_launch',
  '@screensaver_enabled',
  '@screensaver_inactivity_enabled',
  '@screensaver_inactivity_delay',
  '@screensaver_motion_enabled',
  '@screensaver_motion_sensitivity',
  '@screensaver_motion_delay',
  '@screensaver_brightness',
  '@default_brightness',
  '@kiosk_display_mode',
  '@kiosk_external_app_package',
  '@kiosk_external_app_mode',
  '@kiosk_multi_app_appearance',
  '@kiosk_auto_relaunch_app',
  '@kiosk_overlay_button_visible',
  '@kiosk_overlay_button_position',
  '@kiosk_pin_max_attempts',
  '@kiosk_status_bar_enabled',
  '@kiosk_status_bar_on_overlay',
  '@kiosk_status_bar_on_return',
  '@kiosk_status_bar_show_battery',
  '@kiosk_status_bar_show_wifi',
  '@kiosk_status_bar_show_bluetooth',
  '@kiosk_status_bar_show_volume',
  '@kiosk_status_bar_show_time',
  '@kiosk_external_app_test_mode',
  '@kiosk_back_button_mode',
  '@kiosk_back_button_timer_delay',
  '@kiosk_keyboard_mode',
  '@kiosk_screen_orientation',
  // PIN Mode
  '@kiosk_pin_mode',
  // URL Rotation
  '@kiosk_url_rotation_enabled',
  '@kiosk_url_rotation_list',
  '@kiosk_url_rotation_interval',
  '@kiosk_url_rotation_pause_on_touch',
  '@kiosk_url_rotation_resume_delay',
  // URL Planner
  '@kiosk_url_planner_enabled',
  '@kiosk_url_planner_events',
  // REST API
  // Note: @kiosk_rest_api_key is handled separately via Keychain (secure storage)
  // Live MJPEG camera stream
  '@kiosk_allow_power_button',
  '@kiosk_block_factory_reset',
  '@kiosk_allow_notifications',
  '@kiosk_allow_system_info',
  // Return to Settings
  '@kiosk_return_tap_count',
  '@kiosk_return_tap_timeout',
  '@kiosk_return_mode',
  '@kiosk_return_button_position',
  '@kiosk_volume_up_5tap_enabled',
  // Blocking Overlays
  '@kiosk_blocking_overlays_enabled',
  '@kiosk_blocking_overlays_regions',
  // Camera preference for motion detection
  '@motion_camera_position',
  // WebView Back Button
  '@kiosk_webview_back_button_enabled',
  '@kiosk_webview_back_button_x_percent',
  '@kiosk_webview_back_button_y_percent',
  // Auto-Brightness
  '@kiosk_auto_brightness_enabled',
  '@kiosk_auto_brightness_min',
  '@kiosk_auto_brightness_max',
  '@kiosk_auto_brightness_offset',
  '@kiosk_auto_brightness_update_interval',
  '@kiosk_auto_brightness_saved_manual',
  // Brightness Management
  '@brightness_management_enabled',
  // Screen Sleep Scheduler
  '@kiosk_screen_scheduler_enabled',
  '@kiosk_screen_scheduler_rules',
  '@kiosk_screen_scheduler_wake_on_touch',
  // Keep Screen On
  '@kiosk_keep_screen_on',
  // Inactivity Return to Home
  '@kiosk_inactivity_return_enabled',
  '@kiosk_inactivity_return_delay',
  '@kiosk_inactivity_return_reset_on_nav',
  '@kiosk_inactivity_return_clear_cache',
  '@kiosk_inactivity_return_scroll_top',
  // URL Filtering (Blacklist/Whitelist)
  '@kiosk_url_filter_enabled',
  '@kiosk_url_filter_mode',
  '@kiosk_url_filter_list',
  '@kiosk_url_filter_show_feedback',
  // PDF Viewer
  '@kiosk_pdf_viewer_enabled',
  // WebView Zoom Level
  '@kiosk_webview_zoom_level',
  '@kiosk_webview_zoom_mode',
  // Custom User Agent
  '@kiosk_custom_user_agent',

  // Beta Updates
  '@kiosk_beta_updates_enabled',
  // Managed Apps (Multi-App mode, accessibility, keep-alive)
  '@kiosk_managed_apps',

  // Legacy keys
  '@screensaver_delay',
  '@motion_detection_enabled',
  '@motion_sensitivity',
  '@motion_delay',
  // Added: previously-missing settings (media player, lockscreen controls, screensaver
  // media, status bar theme, dashboard, print, and other toggles). Secrets (PIN, REST

  '@kiosk_auto_wake_on_screen_off',
  '@kiosk_dashboard_mode_enabled',
  '@kiosk_dashboard_tiles',
  '@kiosk_default_launcher',
  '@kiosk_disable_overscroll',
  '@kiosk_pull_to_refresh',
  '@kiosk_disable_user_zoom',
  '@kiosk_http_basic_auth_username',
  '@kiosk_intercom_mode',
  '@kiosk_lockscreen_audio_enabled',
  '@kiosk_lockscreen_bluetooth_enabled',
  '@kiosk_lockscreen_brightness_enabled',
  '@kiosk_lockscreen_controls_enabled',
  '@kiosk_lockscreen_emergency_call_enabled',
  '@kiosk_lockscreen_flashlight_enabled',
  '@kiosk_lockscreen_rotation_lock_enabled',
  '@kiosk_lockscreen_wifi_enabled',
  '@kiosk_media_player_autoplay',
  '@kiosk_media_player_bg_color',
  '@kiosk_media_player_fit_mode',
  '@kiosk_media_player_image_duration',
  '@kiosk_media_player_items',
  '@kiosk_media_player_loop',
  '@kiosk_media_player_cache',
  '@kiosk_media_player_unattended',
  '@kiosk_media_player_cache_limit_mb',
  '@kiosk_media_player_mute',
  '@kiosk_media_player_show_controls',
  '@kiosk_media_player_shuffle',
  '@kiosk_media_player_transition',
  '@kiosk_media_player_transition_duration',
  '@kiosk_pause_web_media_when_hidden',
  '@kiosk_pause_page_screen_off',
  '@kiosk_print_enabled',
  '@kiosk_print_paper_size',
  '@kiosk_silent_print_enabled',
  '@kiosk_silent_print_window_print',
  '@kiosk_esc_pos_width_dots',
  '@kiosk_esc_pos_cut',
  '@kiosk_esc_pos_feed_lines',
  '@kiosk_print_origins',
  '@screensaver_type',
  '@screensaver_url',
  '@screensaver_video_items',
  '@screensaver_video_loop',
  '@screensaver_keep_external_app',
  '@screensaver_fade_seconds',
  '@kiosk_language',
  '@kiosk_screen_lock_compat',
  '@kiosk_allow_hardware_screenshot',
  '@kiosk_allow_usb_debug_dialog',
  '@screensaver_proximity_enabled',
  '@screensaver_motion_require_face',
  '@kiosk_restart_button_enabled',
  '@kiosk_restart_button_long_press_seconds',
  '@kiosk_session_reset_button_enabled',
  '@kiosk_dashboard_icon_size',
  '@kiosk_dashboard_nav_auto_hide',
  '@kiosk_dashboard_nav_auto_hide_seconds',
  '@kiosk_dashboard_swipe_between_tiles',
  '@kiosk_dashboard_keep_tiles_loaded',
  '@kiosk_dashboard_start_tile',
  '@kiosk_status_bar_theme',
];

export interface BackupData {
  version: string;
  exportDate: string;
  appVersion: string;
  settings: Record<string, any>;
  hasPinConfigured: boolean;
}

const BACKUP_VERSION = '1.0';

/**
 * Request storage permissions on Android
 */
async function requestStoragePermission(): Promise<boolean> {
  if (Platform.OS !== 'android') {
    return true;
  }

  try {
    // For Android 13+ (API 33+), we don't need READ/WRITE_EXTERNAL_STORAGE for app-specific directories
    // But for Downloads folder, we might need MANAGE_EXTERNAL_STORAGE or use SAF
    // Using app's cache/files directory is simpler and doesn't require special permissions

    // For older Android versions
    if (Platform.Version < 33) {
      const writeGranted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
        {
          title: 'Storage Permission',
          message: 'EasyKiosk needs storage access to save and load backup files.',
          buttonNeutral: 'Ask Me Later',
          buttonNegative: 'Cancel',
          buttonPositive: 'OK',
        }
      );

      const readGranted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
        {
          title: 'Storage Permission',
          message: 'EasyKiosk needs storage access to save and load backup files.',
          buttonNeutral: 'Ask Me Later',
          buttonNegative: 'Cancel',
          buttonPositive: 'OK',
        }
      );

      return (
        writeGranted === PermissionsAndroid.RESULTS.GRANTED &&
        readGranted === PermissionsAndroid.RESULTS.GRANTED
      );
    }

    return true;
  } catch (err) {
    console.error('Permission request error:', err);
    return false;
  }
}

/**
 * Get the backup directory path
 */
function getBackupDirectory(): string {
  // Use Downloads folder for easy access
  return RNFS.DownloadDirectoryPath;
}

/**
 * Generate backup filename with timestamp
 */
function generateBackupFilename(): string {
  const date = new Date();
  const timestamp = date.toISOString().replace(/[:.]/g, '-').slice(0, 19);
  return `EasyKiosk-backup-${timestamp}.json`;
}

/**
 * Collect all settings and build the backup JSON string without writing to disk.
 * Used by the UI to get the content before passing it to SAF for writing.
 */
export async function buildBackupJson(): Promise<{ success: boolean; json?: string; filename?: string; error?: string }> {
  try {
    const settings: Record<string, any> = {};

    for (const key of BACKUP_KEYS) {
      try {
        const value = await AsyncStorage.getItem(key);
        if (value !== null) {
          settings[key] = value;
        }
      } catch (e) {
        console.warn(`Failed to read key ${key}:`, e);
      }
    }





    const hasPinConfigured = await hasSecurePin();

    // The version of the app that writes the file (#363); 'unknown' if the native side cannot say
    let appVersion = 'unknown';
    try {
      appVersion = (await UpdateModule.getCurrentVersion()).versionName || appVersion;
    } catch (e) {
      console.warn('Failed to read the app version for the backup:', e);
    }

    const backupData: BackupData = {
      version: BACKUP_VERSION,
      exportDate: new Date().toISOString(),
      appVersion,
      settings,
      hasPinConfigured,
    };

    return {
      success: true,
      json: JSON.stringify(backupData, null, 2),
      filename: generateBackupFilename(),
    };
  } catch (error) {
    console.error('Build backup JSON error:', error);
    return { success: false, error: String(error) };
  }
}

/**
 * Export current configuration to a backup file in the Downloads folder.
 * @deprecated On Android 10+, use buildBackupJson() + FilePickerModule.saveJsonFile() instead
 * to avoid EACCES permission errors. This function is kept for Android 9 and below.
 */
export async function exportBackup(): Promise<{ success: boolean; filePath?: string; error?: string }> {
  try {
    const hasPermission = await requestStoragePermission();
    if (!hasPermission) {
      return { success: false, error: 'Storage permission denied' };
    }

    const built = await buildBackupJson();
    if (!built.success || !built.json || !built.filename) {
      return { success: false, error: built.error || 'Failed to build backup' };
    }

    const directory = getBackupDirectory();
    const filePath = `${directory}/${built.filename}`;

    const dirExists = await RNFS.exists(directory);
    if (!dirExists) {
      await RNFS.mkdir(directory);
    }

    await RNFS.writeFile(filePath, built.json, 'utf8');
    return { success: true, filePath };
  } catch (error) {
    console.error('Export backup error:', error);
    return { success: false, error: String(error) };
  }
}

/**
 * List available backup files
 */
export async function listBackupFiles(): Promise<{ name: string; path: string; date: string }[]> {
  try {
    const hasPermission = await requestStoragePermission();
    if (!hasPermission) {
      return [];
    }

    const directory = getBackupDirectory();
    const exists = await RNFS.exists(directory);
    if (!exists) {
      return [];
    }

    const files = await RNFS.readDir(directory);
    const backupFiles = files
      .filter(file => /^(?:EasyKiosk|freekiosk)-backup-.*\.json$/i.test(file.name))
      .map(file => ({
        name: file.name,
        path: file.path,
        date: file.mtime ? file.mtime.toISOString() : '',
      }))
      .sort((a, b) => b.date.localeCompare(a.date)); // Most recent first

    return backupFiles;
  } catch (error) {
    console.error('List backup files error:', error);
    return [];
  }
}

/**
 * Read and validate a backup file
 */
export async function readBackupFile(filePath: string): Promise<{ success: boolean; data?: BackupData; error?: string }> {
  try {
    const exists = await RNFS.exists(filePath);
    if (!exists) {
      return { success: false, error: 'Backup file not found' };
    }

    const content = await RNFS.readFile(filePath, 'utf8');
    return parseBackupContent(content);
  } catch (error) {
    console.error('Read backup file error:', error);
    return { success: false, error: String(error) };
  }
}

/**
 * Import configuration from a backup file
 */
export async function importBackup(filePath: string): Promise<{ success: boolean; error?: string; warning?: string }> {
  try {
    return await importBackupFromContent(await RNFS.readFile(filePath, 'utf8'));
  } catch {
    return { success: false, error: 'Unable to read backup file' };
  }
}

/** Only allowlisted settings are restored. Authentication credentials are never imported. */
export async function importBackupFromContent(jsonContent: string, _fileName?: string): Promise<{ success: boolean; error?: string; warning?: string }> {
  const parsed = parseBackupContent(jsonContent);
  if (!parsed.success || !parsed.data) return { success: false, error: parsed.error };
  const pairs = Object.entries(parsed.data.settings).filter(
    (entry): entry is [string, string] => BACKUP_KEYS.includes(entry[0]) && typeof entry[1] === 'string',
  );
  try {
    const previous = await AsyncStorage.multiGet(pairs.map(([key]) => key));
    try {
      await AsyncStorage.multiSet(pairs);
    } catch {
      // Do not claim success after a partially failed restore.
      await AsyncStorage.multiSet(previous.filter((entry): entry is [string, string] => entry[1] !== null));
      await AsyncStorage.multiRemove(previous.filter(([, value]) => value === null).map(([key]) => key));
      throw new Error('Restore failed');
    }
    return { success: true };
  } catch {
    return { success: false, error: 'Unable to apply configuration' };
  }
}

/** Validate once for both preview and restore, before any setting is changed. */
export function parseBackupContent(jsonContent: string): { success: boolean; data?: BackupData; error?: string } {
  try {
    const data = JSON.parse(jsonContent) as BackupData;
    if (!data || typeof data !== 'object' || Array.isArray(data) || typeof data.version !== 'string' || !data.version ||
        !data.settings || typeof data.settings !== 'object' || Array.isArray(data.settings)) {
      return { success: false, error: 'Invalid backup file format' };
    }
    for (const key of ['exportDate', 'appVersion'] as const) {
      if (data[key] !== undefined && typeof data[key] !== 'string') return { success: false, error: 'Invalid backup metadata' };
    }
    const mode = data.settings['@kiosk_display_mode'];
    if (mode != null && !['webview', 'external_app', 'media_player'].includes(mode)) {
      return { success: false, error: 'Invalid display mode' };
    }
    const recognized = Object.entries(data.settings).filter(([key]) => BACKUP_KEYS.includes(key));
    if (!recognized.some(([, value]) => typeof value === 'string') ||
        recognized.some(([, value]) => value !== null && typeof value !== 'string')) {
      return { success: false, error: 'No valid configuration settings found' };
    }
    return { success: true, data };
  } catch {
    return { success: false, error: 'Invalid JSON configuration' };
  }
}

export function getBackupPreview(data: BackupData): { count: number; url: string; displayMode: string } {
  const url = data.settings['@kiosk_url'];
  return {
    count: Object.entries(data.settings).filter(([key, value]) => BACKUP_KEYS.includes(key) && typeof value === 'string').length,
    // Never show URL credentials, query tokens or raw secret-bearing settings on the preview.
    url: typeof url === 'string' ? url.replace(/\/\/[^/@]+@/, '//••••@').split(/[?#]/)[0] : '',
    displayMode: typeof data.settings['@kiosk_display_mode'] === 'string' ? data.settings['@kiosk_display_mode'] : 'webview',
  };
}

/**
 * Delete a backup file
 */
export async function deleteBackupFile(filePath: string): Promise<{ success: boolean; error?: string }> {
  try {
    const exists = await RNFS.exists(filePath);
    if (!exists) {
      return { success: false, error: 'File not found' };
    }

    await RNFS.unlink(filePath);
    return { success: true };
  } catch (error) {
    console.error('Delete backup file error:', error);
    return { success: false, error: String(error) };
  }
}

export default {
  exportBackup,
  importBackup,
  importBackupFromContent,
  parseBackupContent,
  listBackupFiles,
  readBackupFile,
  deleteBackupFile,
};
