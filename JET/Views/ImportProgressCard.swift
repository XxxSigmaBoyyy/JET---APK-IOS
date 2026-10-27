import SwiftUI

/// Glass card listing running / finished imports.
@MainActor
struct ImportProgressCard: View {
    @ObservedObject var coordinator: ImportCoordinator

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label("Import", systemImage: "square.and.arrow.down.on.square")
                    .font(.headline)
                    .foregroundStyle(JetTheme.ink)
                Spacer()
                if coordinator.activeJobs.isEmpty {
                    Button("Done") { withAnimation(.spring) { coordinator.dismissFinished() } }
                        .font(.subheadline.weight(.semibold))
                        .tint(JetTheme.skyDeep)
                }
            }
            ForEach(coordinator.jobs) { job in
                ImportJobRow(job: job)
            }
        }
        .padding(18)
        .glassCard(cornerRadius: 24, material: .regularMaterial)
        .padding(.horizontal, 16)
    }
}

@MainActor
private struct ImportJobRow: View {
    @ObservedObject var job: ImportJob

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Image(systemName: job.error != nil ? "exclamationmark.triangle.fill" : (job.stage == .done ? "checkmark.circle.fill" : "airplane.departure"))
                    .foregroundStyle(job.error != nil ? Color.orange : (job.stage == .done ? Color.green : JetTheme.skyDeep))
                Text(job.fileName)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(JetTheme.ink)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }
            if let err = job.error {
                Text(err).font(.caption).foregroundStyle(Color(red: 0.80, green: 0.35, blue: 0.05))
            } else {
                ProgressView(value: job.progress)
                    .tint(JetTheme.skyDeep)
                Text(doneNote ?? job.stage.title)
                    .font(.caption)
                    .foregroundStyle(JetTheme.inkSoft)
            }
        }
        .animation(.easeInOut, value: job.stage)
    }

    private var doneNote: String? {
        guard job.stage == .done, let r = job.result, !r.isTranslated else { return nil }
        return r.isUnsupported ? "Added to Home · no prebuilt AOT module for this app in this build"
                               : "Added to Home · preview bridge (runtime not linked)"
    }
}
