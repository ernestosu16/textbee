package com.vernu.sms.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.vernu.sms.R;
import com.vernu.sms.helpers.SMSFilterHelper;
import com.vernu.sms.models.SMSFilterRule;

import java.util.ArrayList;
import java.util.List;

public class SMSFilterActivity extends AppCompatActivity {
    private Context mContext;
    private Switch filterEnabledSwitch;
    private RadioGroup filterModeRadioGroup;
    private RadioButton allowListRadio;
    private RadioButton blockListRadio;
    private RecyclerView filterRulesRecyclerView;
    private FloatingActionButton addRuleFab;
    private FilterRulesAdapter adapter;
    private SMSFilterHelper.FilterConfig filterConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_filter);

        mContext = getApplicationContext();
        
        // Initialize views
        ImageButton backButton = findViewById(R.id.backButton);
        filterEnabledSwitch = findViewById(R.id.filterEnabledSwitch);
        filterModeRadioGroup = findViewById(R.id.filterModeRadioGroup);
        allowListRadio = findViewById(R.id.allowListRadio);
        blockListRadio = findViewById(R.id.blockListRadio);
        filterRulesRecyclerView = findViewById(R.id.filterRulesRecyclerView);
        addRuleFab = findViewById(R.id.addRuleFab);

        // Setup back button
        backButton.setOnClickListener(v -> finish());

        // Load filter config
        filterConfig = SMSFilterHelper.loadFilterConfig(mContext);
        
        // Setup RecyclerView
        adapter = new FilterRulesAdapter(filterConfig.getRules());
        filterRulesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        filterRulesRecyclerView.setAdapter(adapter);

        // Load current settings
        filterEnabledSwitch.setChecked(filterConfig.isEnabled());
        // Default to block list if mode is not set
        if (filterConfig.getMode() == SMSFilterHelper.FilterMode.ALLOW_LIST) {
            allowListRadio.setChecked(true);
        } else {
            blockListRadio.setChecked(true);
        }

        // Setup listeners
        filterEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            filterConfig.setEnabled(isChecked);
            saveFilterConfig();
        });

        filterModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.allowListRadio) {
                filterConfig.setMode(SMSFilterHelper.FilterMode.ALLOW_LIST);
            } else {
                filterConfig.setMode(SMSFilterHelper.FilterMode.BLOCK_LIST);
            }
            saveFilterConfig();
        });

        addRuleFab.setOnClickListener(v -> showAddEditRuleDialog(-1));
    }

    private void saveFilterConfig() {
        SMSFilterHelper.saveFilterConfig(mContext, filterConfig);
    }

    private void showAddEditRuleDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_filter_rule, null);
        builder.setView(dialogView);

        TextInputEditText patternEditText = dialogView.findViewById(R.id.patternEditText);
        Spinner filterTargetSpinner = dialogView.findViewById(R.id.filterTargetSpinner);
        Spinner matchTypeSpinner = dialogView.findViewById(R.id.matchTypeSpinner);
        Switch caseSensitiveSwitch = dialogView.findViewById(R.id.caseSensitiveSwitch);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

        // Setup filter target spinner
        String[] filterTargets = {getString(R.string.sender_label), getString(R.string.message_label), getString(R.string.both_label)};
        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterTargets);
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterTargetSpinner.setAdapter(targetAdapter);

        // Setup match type spinner
        String[] matchTypes = {getString(R.string.exact_match_label), getString(R.string.starts_with_label), getString(R.string.ends_with_label), getString(R.string.contains_label)};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, matchTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        matchTypeSpinner.setAdapter(spinnerAdapter);

        // If editing, populate fields
        boolean isEdit = position >= 0;
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        if (isEdit) {
            SMSFilterRule rule = filterConfig.getRules().get(position);
            patternEditText.setText(rule.getPattern());
            filterTargetSpinner.setSelection(rule.getFilterTarget().ordinal());
            matchTypeSpinner.setSelection(rule.getMatchType().ordinal());
            caseSensitiveSwitch.setChecked(rule.isCaseSensitive());
            if (dialogTitle != null) {
                dialogTitle.setText(R.string.edit_filter_rule_title);
            }
        } else {
            // Default to case insensitive
            caseSensitiveSwitch.setChecked(false);
        }

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            String pattern = patternEditText.getText() != null ? patternEditText.getText().toString().trim() : "";
            if (pattern.isEmpty()) {
                Toast.makeText(this, R.string.enter_pattern_toast, Toast.LENGTH_SHORT).show();
                return;
            }

            SMSFilterRule.FilterTarget filterTarget = SMSFilterRule.FilterTarget.values()[filterTargetSpinner.getSelectedItemPosition()];
            SMSFilterRule.MatchType matchType = SMSFilterRule.MatchType.values()[matchTypeSpinner.getSelectedItemPosition()];
            boolean caseSensitive = caseSensitiveSwitch.isChecked();
            
            if (isEdit) {
                SMSFilterRule rule = filterConfig.getRules().get(position);
                rule.setPattern(pattern);
                rule.setFilterTarget(filterTarget);
                rule.setMatchType(matchType);
                rule.setCaseSensitive(caseSensitive);
                adapter.notifyItemChanged(position);
            } else {
                SMSFilterRule newRule = new SMSFilterRule(pattern, matchType, filterTarget, caseSensitive);
                filterConfig.getRules().add(newRule);
                adapter.notifyItemInserted(filterConfig.getRules().size() - 1);
            }

            saveFilterConfig();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteRule(int position) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_rule_title)
            .setMessage(R.string.delete_rule_message)
            .setPositiveButton(R.string.delete_button, (dialog, which) -> {
                filterConfig.getRules().remove(position);
                adapter.notifyItemRemoved(position);
                saveFilterConfig();
            })
            .setNegativeButton(R.string.cancel_button, null)
            .show();
    }

    private class FilterRulesAdapter extends RecyclerView.Adapter<FilterRulesAdapter.ViewHolder> {
        private List<SMSFilterRule> rules;

        public FilterRulesAdapter(List<SMSFilterRule> rules) {
            this.rules = rules != null ? rules : new ArrayList<>();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_filter_rule, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SMSFilterRule rule = rules.get(position);
            holder.patternText.setText(rule.getPattern());
            
            String matchTypeText = "";
            switch (rule.getMatchType()) {
                case EXACT:
                    matchTypeText = getString(R.string.exact_match_label);
                    break;
                case STARTS_WITH:
                    matchTypeText = getString(R.string.starts_with_label);
                    break;
                case ENDS_WITH:
                    matchTypeText = getString(R.string.ends_with_label);
                    break;
                case CONTAINS:
                    matchTypeText = getString(R.string.contains_label);
                    break;
            }
            holder.matchTypeText.setText(matchTypeText);

            String filterTargetText = "";
            switch (rule.getFilterTarget()) {
                case SENDER:
                    filterTargetText = getString(R.string.filter_sender_label);
                    break;
                case MESSAGE:
                    filterTargetText = getString(R.string.filter_message_label);
                    break;
                case BOTH:
                    filterTargetText = getString(R.string.filter_both_label);
                    break;
            }
            String caseText = rule.isCaseSensitive() ? getString(R.string.case_sensitive_suffix) : getString(R.string.case_insensitive_suffix);
            holder.filterTargetText.setText(getString(R.string.filter_target_with_case, filterTargetText, caseText));

            holder.editButton.setOnClickListener(v -> showAddEditRuleDialog(position));
            holder.deleteButton.setOnClickListener(v -> deleteRule(position));
        }

        @Override
        public int getItemCount() {
            return rules.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView patternText;
            TextView matchTypeText;
            TextView filterTargetText;
            ImageButton editButton;
            ImageButton deleteButton;

            ViewHolder(View itemView) {
                super(itemView);
                patternText = itemView.findViewById(R.id.patternText);
                matchTypeText = itemView.findViewById(R.id.matchTypeText);
                filterTargetText = itemView.findViewById(R.id.filterTargetText);
                editButton = itemView.findViewById(R.id.editButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
}
