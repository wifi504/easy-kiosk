import React, { useState, useEffect, useRef } from 'react';
import { View, StyleSheet, TouchableOpacity, Text, NativeModules, BackHandler, ScrollView, Alert, ActivityIndicator } from 'react-native';
import PinInput from '../components/PinInput';
import { DEFAULT_PIN } from '../utils/authPolicy';
import { StorageService } from '../utils/storage';
import { migrateOldPin, hasSecurePin } from '../utils/secureStorage';
import AppLauncherModule from '../utils/AppLauncherModule';
import FilePickerModule from '../utils/FilePickerModule';
import { parseBackupContent, importBackupFromContent, getBackupPreview, BackupData } from '../utils/BackupService';
import { grantSettingsAccess, revokeSettingsAccess } from '../utils/authState';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/AppNavigator';
import { useTranslation } from 'react-i18next';

type PinScreenNavigationProp = NativeStackNavigationProp<RootStackParamList, 'Pin'>;
interface PinScreenProps {
  navigation: PinScreenNavigationProp;
  route?: { params?: { initialTab?: string } };
}
interface SelectedConfiguration { name: string; content: string; data: BackupData }

const PinScreen: React.FC<PinScreenProps> = ({ navigation, route }) => {
  const initialTab = route?.params?.initialTab;
  const { t } = useTranslation();
  const [view, setView] = useState<'import' | 'admin'>('import');
  const [selected, setSelected] = useState<SelectedConfiguration | null>(null);
  const selectionRef = useRef<SelectedConfiguration | null>(null);
  const viewRef = useRef(view);
  const [busy, setBusy] = useState(false);
  const [migrationDone, setMigrationDone] = useState(false);
  const [displayMode, setDisplayMode] = useState<'webview' | 'external_app' | 'media_player'>('webview');
  const [externalAppPackage, setExternalAppPackage] = useState<string | null>(null);

  useEffect(() => { migrateFromOldSystem(); loadDisplayMode(); }, []);
  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (!busy) { revokeSettingsAccess(); navigation.navigate('Kiosk'); }
      return true;
    });
    return () => sub.remove();
  }, [navigation, busy]);

  const loadDisplayMode = async (): Promise<void> => {
    try {
      setDisplayMode(await StorageService.getDisplayMode());
      setExternalAppPackage(await StorageService.getExternalAppPackage());
    } catch (error) { console.error('[PinScreen] Failed to load display mode:', error); }
  };
  const migrateFromOldSystem = async (): Promise<void> => {
    try {
      if (!(await hasSecurePin())) {
        const oldPin = await StorageService.getPin();
        await migrateOldPin(oldPin);
        if (oldPin && oldPin !== '1234') await StorageService.savePin('');
      }
    } catch (error) { console.warn('[PinScreen] PIN migration unavailable:', error); }
    finally { setMigrationDone(true); }
  };

  const openAdmin = () => {
    if (busy) return;
    selectionRef.current = null;
    setSelected(null);
    viewRef.current = 'admin';
    setView('admin');
  };
  const chooseConfiguration = async () => {
    if (busy) return;
    setBusy(true);
    selectionRef.current = null;
    setSelected(null);
    viewRef.current = 'import';
    setView('import');
    try {
      const file = await FilePickerModule.pickJsonFile();
      const parsed = parseBackupContent(file.content);
      if (!parsed.success || !parsed.data) {
        Alert.alert(t('components.backupRestore.invalidBackup'), t('components.backupRestore.notValidBackup'));
        return;
      }
      const choice = { name: file.name, content: file.content, data: parsed.data };
      selectionRef.current = choice;
      setSelected(choice);
    } catch (error: any) {
      if (error?.code !== 'PICKER_CANCELLED') Alert.alert(t('components.backupRestore.error'), t('configurationEntry.readFailed'));
    } finally { setBusy(false); }
  };

  const handleSuccess = async (): Promise<void> => {
    // A completed verification must never authorize a different file or screen.
    if (viewRef.current !== view) return;
    if (view === 'admin') {
      grantSettingsAccess();
      navigation.navigate('Settings', initialTab ? { initialTab } : undefined);
      return;
    }
    if (!selected || selectionRef.current !== selected) return;
    const result = await importBackupFromContent(selected.content, selected.name);
    if (!result.success) {
      Alert.alert(t('components.backupRestore.restoreFailedTitle'), t('configurationEntry.applyFailed'));
      return;
    }
    revokeSettingsAccess();
    navigation.reset({ index: 0, routes: [{ name: 'Kiosk' }] });
  };

  const handleBack = async (): Promise<void> => {
    if (busy) return;
    // If in external app mode, relaunch the external app with overlay service
    if (displayMode === 'external_app' && externalAppPackage) {
      try {
        // Load return settings
        const returnTapCount = await StorageService.getReturnTapCount();
        const returnTapTimeout = await StorageService.getReturnTapTimeout();
        const returnMode = await StorageService.getReturnMode();
        const returnButtonPosition = await StorageService.getReturnButtonPosition();
        const autoRelaunch = await StorageService.getAutoRelaunchApp();

        // Start OverlayService BEFORE launching the external app
        const { OverlayServiceModule } = NativeModules;
        const nfcEnabled = await StorageService.getAllowNotifications();
        await OverlayServiceModule.startOverlayService(
          returnTapCount, 
          returnTapTimeout, 
          returnMode, 
          returnButtonPosition,
          externalAppPackage,
          autoRelaunch,
          nfcEnabled
        );
        console.log('[PinScreen] OverlayService started with auto-relaunch monitoring');

        await AppLauncherModule.launchExternalApp(externalAppPackage);
      } catch (error) {
        console.error('[PinScreen] Failed to relaunch external app:', error);
      }
    }
    // Navigate back to Kiosk screen
    revokeSettingsAccess();
    navigation.navigate('Kiosk');
  };


  const preview = selected ? getBackupPreview(selected.data) : null;
  const modeLabel = preview?.displayMode === 'external_app' ? t('configurationEntry.externalApp')
    : preview?.displayMode === 'media_player' ? t('configurationEntry.mediaPlayer') : t('configurationEntry.browser');
  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity testID="return-kiosk" style={styles.headerButton} onPress={handleBack} disabled={busy}>
          <Text style={styles.headerButtonText}>{t('general.backToKiosk')}</Text>
        </TouchableOpacity>
        <TouchableOpacity testID="open-administration" style={styles.headerButton} onPress={openAdmin} disabled={busy}>
          <Text style={styles.headerButtonText}>{t('configurationEntry.admin')}</Text>
        </TouchableOpacity>
      </View>
      <ScrollView contentContainerStyle={styles.body} keyboardShouldPersistTaps="handled">
        <TouchableOpacity testID="select-configuration" style={styles.chooseButton} onPress={chooseConfiguration} disabled={busy}>
          <Text style={styles.chooseButtonText}>{t('configurationEntry.choose')}</Text>
        </TouchableOpacity>
        {selected && preview && view === 'import' && (
          <View testID="configuration-preview" style={styles.preview}>
            <Text style={styles.previewTitle}>{selected.name}</Text>
            <Text>{t('configurationEntry.mode', { value: modeLabel })}</Text>
            {!!preview.url && <Text>{t('configurationEntry.url', { value: preview.url })}</Text>}
            <Text>{t('configurationEntry.count', { count: preview.count })}</Text>
            {!!selected.data.appVersion && <Text>{t('configurationEntry.version', { value: selected.data.appVersion })}</Text>}
            {!!selected.data.exportDate && <Text>{t('configurationEntry.exported', { value: selected.data.exportDate })}</Text>}
          </View>
        )}
        {migrationDone && (view === 'admin' || selected !== null) && (
          <PinInput key={view + (selected?.name || '')} storedPin={DEFAULT_PIN} onSuccess={handleSuccess}
            onBusyChange={setBusy} submitLabel={view === 'import' ? t('configurationEntry.confirmImport') : undefined} />
        )}
        {busy && !selected && view === 'import' && <ActivityIndicator style={styles.spinner} />}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  header: { paddingTop: 40, paddingHorizontal: 20 },
  headerButton: {
    backgroundColor: '#fff', paddingVertical: 15, borderRadius: 10, borderWidth: 2, borderColor: '#ddd',
    shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 3, elevation: 3, marginBottom: 10,
  },
  headerButtonText: { color: '#666', fontSize: 18, fontWeight: '600', textAlign: 'center' },
  body: { flexGrow: 1, justifyContent: 'center', padding: 20 },
  chooseButton: { alignSelf: 'center', backgroundColor: '#2b7fff', borderRadius: 8, paddingHorizontal: 24, paddingVertical: 16, marginVertical: 16 },
  chooseButtonText: { color: '#fff', fontSize: 18, fontWeight: '600' },
  preview: { backgroundColor: '#fff', borderRadius: 10, padding: 20, gap: 10 },
  previewTitle: { fontSize: 18, fontWeight: '600', color: '#333' },
  spinner: { margin: 20 },
});

export default PinScreen;
