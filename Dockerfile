FROM eclipse-temurin:17-jdk-jammy AS build

ARG GRADLE_VERSION=9.6.0
ARG ANDROID_CMDLINE_TOOLS=13114758
ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    PATH=/opt/gradle/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:$PATH

RUN apt-get update && apt-get install -y --no-install-recommends curl unzip ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && curl --retry 4 --retry-all-errors -fsSL "https://downloads.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -s "/opt/gradle-${GRADLE_VERSION}" /opt/gradle \
    && mkdir -p "$ANDROID_HOME/cmdline-tools" \
    && curl --retry 4 --retry-all-errors -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS}_latest.zip" -o /tmp/tools.zip \
    && unzip -q /tmp/tools.zip -d /tmp/android-tools \
    && mv /tmp/android-tools/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest" \
    && rm /tmp/gradle.zip /tmp/tools.zip

RUN yes | sdkmanager --licenses >/dev/null \
    && sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"

WORKDIR /workspace
COPY . .

FROM build AS release-build
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=secret,id=ritmo_keystore,required=true \
    --mount=type=secret,id=ritmo_signing_properties,required=true \
    RITMO_SIGNING_KEY=/run/secrets/ritmo_keystore \
    RITMO_SIGNING_PROPERTIES=/run/secrets/ritmo_signing_properties \
    gradle --no-daemon testDebugUnitTest assembleRelease \
    && /opt/android-sdk/build-tools/36.0.0/apksigner verify --verbose /workspace/app/build/outputs/apk/release/app-release.apk

FROM scratch AS release
COPY --from=release-build /workspace/app/build/outputs/apk/release/app-release.apk /ritmo-release.apk

FROM build AS debug-build
RUN --mount=type=cache,target=/root/.gradle \
    gradle --no-daemon testDebugUnitTest assembleDebug

FROM scratch AS export
COPY --from=debug-build /workspace/app/build/outputs/apk/debug/app-debug.apk /ritmo-debug.apk
