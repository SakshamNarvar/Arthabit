import React, { useEffect, useState } from 'react';
import {
  View,
  StyleSheet,
  Image,
  ScrollView,
  TouchableOpacity,
  Platform,
  ImageStyle,
  ActivityIndicator,
} from 'react-native';
import CustomText from '../components/CustomText';
import { UserDto } from './dto/UserDto';
import { theme } from '../theme/theme';
import { Camera, ChevronRight, User, Phone, Mail, Bell, Lock, Moon, LogOut } from 'lucide-react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/AppNavigator';
import API_CONFIG from '../config/apiConfig';

interface ProfileItemProps {
  icon: React.ReactNode;
  label: string;
  value: string;
}

const ProfileItem: React.FC<ProfileItemProps> = ({ icon, label, value }) => (
  <View style={styles.profileItem}>
    <View style={styles.iconContainer}>
      {icon}
    </View>
    <View style={styles.itemContent}>
      <CustomText style={styles.label}>{label}</CustomText>
      <CustomText style={styles.value}>{value}</CustomText>
    </View>
    <ChevronRight color={theme.colors.text.secondary} size={16} />
  </View>
);

const Profile = () => {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const [user, setUser] = useState<UserDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchUserProfile = async () => {
      try {
        const userId = await AsyncStorage.getItem('userId');
        if (!userId) {
          console.error('No userId found in storage');
          setIsLoading(false);
          return;
        }
        const SERVER_BASE_URL = API_CONFIG.USER_SERVICE_URL;
        const response = await fetch(`${SERVER_BASE_URL}/user/v1/getUser?userId=${userId}`, {
          method: 'GET',
          headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json',
          },
        });
        if (response.ok) {
          const data = await response.json();
          setUser({
            userId: data['user_id'],
            firstName: data['first_name'],
            lastName: data['last_name'],
            phoneNumber: data['phone_number'],
            email: data['email'],
            profilePic: data['profile_pic'] || undefined,
          });
        } else {
          console.error('Failed to fetch user profile:', response.status);
        }
      } catch (error) {
        console.error('Error fetching user profile:', error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchUserProfile();
  }, []);

  const handleLogout = async () => {
    await AsyncStorage.removeItem('accessToken');
    await AsyncStorage.removeItem('refreshToken');
    await AsyncStorage.removeItem('userId');
    navigation.reset({index: 0, routes: [{name: 'Login'}]});
  };

  const formatPhoneNumber = (phone: number): string => {
    const phoneStr = phone.toString();
    return `(${phoneStr.slice(0, 3)}) ${phoneStr.slice(3, 6)}-${phoneStr.slice(6)}`;
  };

  return (
    <ScrollView style={styles.container}>
      {isLoading ? (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={theme.colors.primary} />
        </View>
      ) : (
        <>
          <View style={styles.header}>
            <View style={styles.profileImageContainer}>
              <Image
                source={{ uri: user?.profilePic || 'https://i.pravatar.cc/300' }}
                style={styles.profileImage as ImageStyle}
              />
              <TouchableOpacity style={styles.editButton}>
                <Camera color={theme.colors.primary} size={16} />
              </TouchableOpacity>
            </View>
            <CustomText style={styles.name}>
              {user ? `${user.firstName} ${user.lastName}` : 'Unknown User'}
            </CustomText>
            <CustomText style={styles.userId}>ID: {user?.userId || 'N/A'}</CustomText>
          </View>

          <View style={styles.content}>
            <View style={styles.section}>
              <CustomText style={styles.sectionTitle}>Personal Information</CustomText>
              <View style={styles.card}>
                <ProfileItem
                  icon={<User color={theme.colors.primary} size={16} />}
                  label="Name"
                  value={user ? `${user.firstName} ${user.lastName}` : 'N/A'}
                />
                <ProfileItem
                  icon={<Phone color={theme.colors.primary} size={16} />}
                  label="Phone"
                  value={user ? formatPhoneNumber(user.phoneNumber) : 'N/A'}
                />
                <ProfileItem
                  icon={<Mail color={theme.colors.primary} size={16} />}
                  label="Email"
                  value={user?.email || 'N/A'}
                />
              </View>
            </View>

        <View style={styles.section}>
          <CustomText style={styles.sectionTitle}>Settings</CustomText>
          <View style={styles.card}>
            <ProfileItem
              icon={<Bell color={theme.colors.primary} size={16} />}
              label="Notifications"
              value="On"
            />
            <ProfileItem
              icon={<Lock color={theme.colors.primary} size={16} />}
              label="Privacy"
              value="View Settings"
            />
            <ProfileItem
              icon={<Moon color={theme.colors.primary} size={16} />}
              label="Dark Mode"
              value="System"
            />
          </View>
        </View>

        <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
          <LogOut color={theme.colors.status.error} size={20} />
          <CustomText style={styles.logoutText}>Logout</CustomText>
        </TouchableOpacity>
      </View>
        </>
      )}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.background.primary,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingTop: 100,
  },
  header: {
    alignItems: 'center',
    paddingVertical: theme.spacing['2xl'],
    backgroundColor: theme.colors.surface.primary,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.effects.glass.borderColor,
  },
  profileImageContainer: {
    position: 'relative',
    marginBottom: theme.spacing.lg,
  },
  profileImage: {
    width: 120,
    height: 120,
    borderRadius: 60,
    borderWidth: 3,
    borderColor: theme.colors.primary,
  },
  editButton: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    backgroundColor: theme.colors.surface.secondary,
    borderRadius: theme.borderRadius.full,
    padding: theme.spacing.sm,
    borderWidth: 2,
    borderColor: theme.colors.primary,
  },
  name: {
    fontSize: theme.typography.fontSize['2xl'],
    fontWeight: theme.typography.fontWeight.bold,
    color: theme.colors.text.primary,
    marginBottom: theme.spacing.xs,
  },
  userId: {
    fontSize: theme.typography.fontSize.sm,
    color: theme.colors.text.secondary,
  },
  content: {
    padding: theme.spacing.lg,
  },
  section: {
    marginBottom: theme.spacing.xl,
  },
  sectionTitle: {
    fontSize: theme.typography.fontSize.lg,
    fontWeight: theme.typography.fontWeight.semibold,
    color: theme.colors.text.primary,
    marginBottom: theme.spacing.md,
    marginLeft: theme.spacing.sm,
  },
  card: {
    backgroundColor: theme.colors.surface.primary,
    borderRadius: theme.borderRadius.lg,
    ...Platform.select({
      ios: {
        shadowColor: theme.colors.effects.shadow.medium.color,
        shadowOffset: theme.colors.effects.shadow.medium.offset,
        shadowOpacity: theme.colors.effects.shadow.medium.opacity,
        shadowRadius: theme.colors.effects.shadow.medium.radius,
      },
      android: {
        elevation: 4,
      },
    }),
    borderWidth: 1,
    borderColor: theme.colors.effects.glass.borderColor,
  },
  profileItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: theme.spacing.lg,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.effects.glass.borderColor,
  },
  iconContainer: {
    width: 32,
    height: 32,
    borderRadius: theme.borderRadius.full,
    backgroundColor: theme.colors.surface.elevated,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: theme.spacing.md,
  },
  itemContent: {
    flex: 1,
  },
  label: {
    fontSize: theme.typography.fontSize.sm,
    color: theme.colors.text.secondary,
    marginBottom: 2,
  },
  value: {
    fontSize: theme.typography.fontSize.base,
    color: theme.colors.text.primary,
    fontWeight: theme.typography.fontWeight.medium,
  },
  logoutButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.colors.surface.primary,
    borderRadius: theme.borderRadius.lg,
    padding: theme.spacing.lg,
    marginBottom: theme.spacing['2xl'],
    borderWidth: 1,
    borderColor: theme.colors.status.error,
  },
  logoutText: {
    fontSize: theme.typography.fontSize.base,
    fontWeight: theme.typography.fontWeight.semibold,
    color: theme.colors.status.error,
    marginLeft: theme.spacing.sm,
  },
});

export default Profile;