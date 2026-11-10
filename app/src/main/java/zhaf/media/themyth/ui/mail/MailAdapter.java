package zhaf.media.themyth.ui.mail;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import zhaf.media.themyth.R;
import zhaf.media.themyth.core.Texts;
import zhaf.media.themyth.data.social.MailLogic;
import zhaf.media.themyth.data.social.MailMessage;
import zhaf.media.themyth.data.social.SocialDay;
import zhaf.media.themyth.databinding.ItemMailBinding;

public class MailAdapter extends RecyclerView.Adapter<MailAdapter.Holder> {

    public interface Listener {
        void onMailClick(MailMessage mail);

        void onClaim(MailMessage mail);
    }

    private final List<MailMessage> mails = new ArrayList<>();
    private final Listener listener;
    private String busyId;

    public MailAdapter(Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submit(List<MailMessage> data) {
        mails.clear();
        if (data != null) {
            mails.addAll(data);
        }
        notifyDataSetChanged();
    }

    public void setBusyId(String busyId) {
        this.busyId = busyId;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return mails.get(position).id.hashCode();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemMailBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(mails.get(position), busyId, listener);
    }

    @Override
    public int getItemCount() {
        return mails.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemMailBinding binding;

        Holder(ItemMailBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MailMessage mail, String busyId, Listener listener) {
            Context context = binding.getRoot().getContext();
            long now = System.currentTimeMillis();
            boolean expired = MailLogic.isExpired(mail.expiresAt, now);

            binding.tvMailType.setText(typeLabel(context, mail.type));
            binding.tvMailTitle.setText(mail.displayTitle());
            binding.tvMailBody.setText(mail.displayBody());

            if (mail.hasAttachment()) {
                binding.tvMailAttachment.setVisibility(View.VISIBLE);
                binding.tvMailAttachment.setText(attachmentSummary(context, mail));
            } else {
                binding.tvMailAttachment.setVisibility(View.GONE);
            }

            if (expired) {
                binding.tvMailExpiry.setText(R.string.mail_expired);
                binding.tvMailExpiry.setTextColor(
                        ContextCompat.getColor(context, R.color.danger));
            } else {
                binding.tvMailExpiry.setText(context.getString(R.string.mail_expires,
                        SocialDay.daysLeft(mail.expiresAt, now)));
                binding.tvMailExpiry.setTextColor(
                        ContextCompat.getColor(context, R.color.text_muted));
            }

            if (mail.claimed) {
                binding.tvMailStatus.setText(R.string.mail_claimed);
                binding.tvMailStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.success));
            } else if (!mail.hasAttachment()) {
                binding.tvMailStatus.setText("");
            } else if (expired) {
                binding.tvMailStatus.setText(R.string.mail_expired);
                binding.tvMailStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.danger));
            } else {
                binding.tvMailStatus.setText("");
            }

            boolean canClaim = mail.canClaim(now);
            binding.btnMailClaim.setVisibility(canClaim ? View.VISIBLE : View.GONE);
            binding.btnMailClaim.setEnabled(busyId == null || !busyId.equals(mail.id));
            binding.btnMailClaim.setOnClickListener(v -> listener.onClaim(mail));

            binding.getRoot().setAlpha(mail.read ? 0.75f : 1f);
            binding.getRoot().setOnClickListener(v -> listener.onMailClick(mail));
        }

        private static String attachmentSummary(Context context, MailMessage mail) {
            StringBuilder builder = new StringBuilder();
            if (mail.coin > 0) {
                builder.append(context.getString(R.string.mail_attachment_coin,
                        Texts.number(mail.coin)));
            }
            if (mail.diamond > 0) {
                if (builder.length() > 0) {
                    builder.append(context.getString(R.string.mail_attachment_sep));
                }
                builder.append(context.getString(R.string.mail_attachment_diamond,
                        Texts.number(mail.diamond)));
            }
            int itemCount = 0;
            for (Map.Entry<String, Integer> entry : mail.items.entrySet()) {
                itemCount += entry.getValue() == null ? 0 : entry.getValue();
            }
            if (itemCount > 0) {
                if (builder.length() > 0) {
                    builder.append(context.getString(R.string.mail_attachment_sep));
                }
                builder.append(context.getString(R.string.mail_attachment_items, itemCount));
            }
            return builder.toString();
        }

        private static String typeLabel(Context context, String type) {
            switch (type == null ? "" : type) {
                case "WELCOME":
                    return context.getString(R.string.mail_type_welcome);
                case "EVENT":
                    return context.getString(R.string.mail_type_event);
                case "REWARD":
                    return context.getString(R.string.mail_type_reward);
                case "RANKING":
                    return context.getString(R.string.mail_type_ranking);
                case "COMPENSATION":
                    return context.getString(R.string.mail_type_compensation);
                default:
                    return context.getString(R.string.mail_type_system);
            }
        }
    }
}
