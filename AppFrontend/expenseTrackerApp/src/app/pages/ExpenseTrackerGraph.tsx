import React, {useEffect, useRef} from 'react';
import {View, Animated, StyleSheet} from 'react-native';
import Svg, {Circle, G} from 'react-native-svg';
import CustomText from '../components/CustomText';

const AnimatedCircle = Animated.createAnimatedComponent(Circle);

interface CircularProgressProps {
  radius?: number;
  strokeWidth?: number;
  value?: number;
  activeColor?: string;
  inactiveColor?: string;
  duration?: number;
}

const ExpenseTrackerGraph: React.FC<CircularProgressProps> = ({
  radius = 90,
  strokeWidth = 12,
  value = 85,
  activeColor = '#2ecc71',
  inactiveColor = 'rgba(46, 204, 113, 0.2)',
  duration = 1500,
}) => {
  const animatedValue = useRef(new Animated.Value(0)).current;
  const circumference = 2 * Math.PI * radius;

  useEffect(() => {
    Animated.timing(animatedValue, {
      toValue: value,
      duration,
      useNativeDriver: false,
    }).start();
  }, [value]);

  const strokeDashoffset = animatedValue.interpolate({
    inputRange: [0, 100],
    outputRange: [circumference, 0],
    extrapolate: 'clamp',
  });

  const svgSize = (radius + strokeWidth) * 2;

  return (
    <View style={styles.container}>
      <Svg width={svgSize} height={svgSize}>
        <G rotation="-90" origin={`${svgSize / 2}, ${svgSize / 2}`}>
          <Circle
            cx={svgSize / 2}
            cy={svgSize / 2}
            r={radius}
            stroke={inactiveColor}
            strokeWidth={strokeWidth}
            fill="none"
          />
          <AnimatedCircle
            cx={svgSize / 2}
            cy={svgSize / 2}
            r={radius}
            stroke={activeColor}
            strokeWidth={strokeWidth}
            fill="none"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
          />
        </G>
      </Svg>
      <View style={styles.labelContainer}>
        <CustomText style={styles.valueText}>{value}%</CustomText>
        <CustomText style={styles.subtitleText}>spent</CustomText>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 20,
  },
  labelContainer: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
  },
  valueText: {
    fontSize: 28,
    fontWeight: '700',
    color: '#222',
  },
  subtitleText: {
    fontSize: 14,
    color: '#888',
    marginTop: 2,
  },
});

export default ExpenseTrackerGraph;