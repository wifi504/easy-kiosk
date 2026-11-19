import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import KioskScreen from '../screens/KioskScreen';
// v1.2: Use new settings screen with Material tabs

export type RootStackParamList = {
  Kiosk: undefined;

  Pin: { initialTab?: string } | undefined;
  Settings: { initialTab?: string } | undefined;
  BlockingOverlays: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();
// Management screens load only after the user asks to enter them.
const getPinScreen = () => require('../screens/PinScreen').default;
const getSettingsScreen = () => require('../screens/settings').SettingsScreen;
const getBlockingOverlaysScreen = () => require('../screens/settings/BlockingOverlaysScreen').default;

const AppNavigator: React.FC = () => {
  return (
    <NavigationContainer>
      <Stack.Navigator
        initialRouteName="Kiosk"
        screenOptions={{
          headerShown: false,
        }}
      >
        <Stack.Screen 
          name="Kiosk" 
          component={KioskScreen}
          options={{
            headerShown: false,
          }}
        />
        <Stack.Screen 
          name="Pin" 
          getComponent={getPinScreen}
          options={{
            headerShown: false,
            gestureEnabled: false,
          }}
        />
        <Stack.Screen 
          name="Settings" 
          getComponent={getSettingsScreen}
          options={{
            headerShown: false, // v1.2: Custom header with tabs
            gestureEnabled: false,
          }}
        />
        <Stack.Screen 
          name="BlockingOverlays" 
          getComponent={getBlockingOverlaysScreen}
          options={{
            headerShown: false,
          }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

export default AppNavigator;
