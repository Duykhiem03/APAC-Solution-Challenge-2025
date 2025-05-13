const stepTrackingSchema = {
  steps: {
    // Document ID format: {userId}_{date}
    _id: String,
    userId: String,
    date: Date,
    steps: Number,
    duration: Number, // in milliseconds
    lastUpdated: Date,
    isPublic: Boolean, // whether this shows up in leaderboards
    goal: {
      type: Number,
      default: 10000
    }
  },

  achievements: {
    // Document ID format: {userId}_{achievementId}
    _id: String,
    userId: String,
    achievementId: String,
    name: String,
    description: String,
    iconUrl: String,
    unlockedAt: Date,
    progress: Number, // 0-100
    category: {
      type: String,
      enum: ['DAILY', 'WEEKLY', 'MONTHLY', 'SPECIAL']
    }
  },

  monthlyStats: {
    // Document ID format: {userId}_{year}_{month}
    _id: String,
    userId: String,
    year: Number,
    month: Number, // 1-12
    totalSteps: Number,
    totalDuration: Number,
    averageSteps: Number,
    bestDay: {
      date: Date,
      steps: Number
    },
    achievements: [{
      achievementId: String,
      unlockedAt: Date
    }]
  }
};

module.exports = { stepTrackingSchema };
