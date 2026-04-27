package uri.app.kickflip;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

public class BoardCustomizerFragment extends Fragment {

    private static final String PREFS = "kickflip_profile";

    private BoardView boardView;

    // Tab views
    private TextView tabShape, tabColors, tabStickers, tabDraw;
    private View tabIndicator;

    // Panels
    private View panelShape, panelColors, panelStickers, panelDraw;

    // Colors panel
    private TextView btnTargetDeck, btnTargetTrucks, btnTargetWheels, btnTargetGrip;
    private ConstraintLayout rowGripStyle;
    private TextView btnGripNone, btnGripBlack, btnGripColored;
    private LinearLayout llColorPalette;

    // Stickers panel
    private GridLayout gridStickers;
    private TextView btnSizeS, btnSizeM, btnSizeL;

    // Draw panel
    private TextView btnBrushS, btnBrushM, btnBrushL;
    private LinearLayout llDrawPalette;

    // Colors panel target: 0=deck,1=trucks,2=wheels,3=grip
    private int colorTarget = 0;

    private static final int[] PALETTE = {
            0xFF3B82F6, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444,
            0xFF8B5CF6, 0xFFEC4899, 0xFFF97316, 0xFF14B8A6,
            0xFFFBBF24, 0xFFFFFFFF, 0xFF0A0A0A, 0xFF6B7280,
            0xFF991B1B, 0xFF065F46, 0xFF1E3A5F, 0xFF7C3AED,
    };

    private static final String[] STICKER_EMOJIS = {
            "⭐", "💀", "⚡", "🔥", "👑", "❤️", "💎", "✚"
    };
    private static final int[] STICKER_TYPES = {
            BoardView.STICKER_STAR, BoardView.STICKER_SKULL,
            BoardView.STICKER_LIGHTNING, BoardView.STICKER_FIRE,
            BoardView.STICKER_CROWN, BoardView.STICKER_HEART,
            BoardView.STICKER_DIAMOND, BoardView.STICKER_CROSS
    };

    private static final String[] SHAPE_LABELS = { "Popsicle", "Old School", "Cruiser", "Fish" };
    private static final int[] SHAPE_IDS = {
            BoardView.SHAPE_POPSICLE, BoardView.SHAPE_OLDSCHOOL,
            BoardView.SHAPE_CRUISER, BoardView.SHAPE_FISH
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_board_customizer, container, false);
        rootView = view;

        boardView    = view.findViewById(R.id.board_view);
        tabShape     = view.findViewById(R.id.tab_shape);
        tabColors    = view.findViewById(R.id.tab_colors);
        tabStickers  = view.findViewById(R.id.tab_stickers);
        tabDraw      = view.findViewById(R.id.tab_draw);
        tabIndicator = view.findViewById(R.id.tab_indicator);
        panelShape   = view.findViewById(R.id.panel_shape);
        panelColors  = view.findViewById(R.id.panel_colors);
        panelStickers= view.findViewById(R.id.panel_stickers);
        panelDraw    = view.findViewById(R.id.panel_draw);

        // Colors panel
        btnTargetDeck   = view.findViewById(R.id.btn_target_deck);
        btnTargetTrucks = view.findViewById(R.id.btn_target_trucks);
        btnTargetWheels = view.findViewById(R.id.btn_target_wheels);
        btnTargetGrip   = view.findViewById(R.id.btn_target_grip);
        rowGripStyle    = view.findViewById(R.id.row_grip_style);
        btnGripNone     = view.findViewById(R.id.btn_grip_none);
        btnGripBlack    = view.findViewById(R.id.btn_grip_black);
        btnGripColored  = view.findViewById(R.id.btn_grip_colored);
        llColorPalette  = view.findViewById(R.id.ll_color_palette);

        // Stickers panel
        gridStickers = view.findViewById(R.id.grid_stickers);
        btnSizeS     = view.findViewById(R.id.btn_size_s);
        btnSizeM     = view.findViewById(R.id.btn_size_m);
        btnSizeL     = view.findViewById(R.id.btn_size_l);

        // Draw panel
        btnBrushS    = view.findViewById(R.id.btn_brush_s);
        btnBrushM    = view.findViewById(R.id.btn_brush_m);
        btnBrushL    = view.findViewById(R.id.btn_brush_l);
        llDrawPalette= view.findViewById(R.id.ll_draw_palette);

        loadSavedBoard();
        populateShapePanel(view);
        populateColorPalette(llColorPalette, false);
        populateColorPalette(llDrawPalette, true);
        populateStickerGrid();
        setupTabs();
        setupColorsPanel();
        setupStickersPanel();
        setupDrawPanel();

        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        view.findViewById(R.id.btn_save).setOnClickListener(v -> saveBoard());

        return view;
    }

    // ── Load / Save ──────────────────────────────────────────────────────────

    private void loadSavedBoard() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE);
        boardView.setShape(prefs.getInt("board_shape", BoardView.SHAPE_POPSICLE));
        boardView.setFillColor(prefs.getInt("deck_color", 0xFF3B82F6));
        boardView.setTruckColor(prefs.getInt("truck_color", 0xFF888888));
        boardView.setWheelColor(prefs.getInt("wheel_color", 0xFFEEEEEE));
        boardView.setGripStyle(prefs.getInt("grip_style", BoardView.GRIP_BLACK));
        boardView.setGripColor(prefs.getInt("grip_color", 0xFF222222));
    }

    private void saveBoard() {
        requireActivity().getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
                .edit()
                .putInt("board_shape", boardView.getBoardShape())
                .putInt("deck_color",  boardView.getFillColor())
                .putInt("truck_color", boardView.getTruckColor())
                .putInt("wheel_color", boardView.getWheelColor())
                .putInt("grip_style",  boardView.getGripStyle())
                .putInt("grip_color",  boardView.getGripColor())
                .apply();
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }

    // ── Shape panel ──────────────────────────────────────────────────────────

    private void populateShapePanel(View root) {
        LinearLayout ll = root.findViewById(R.id.ll_shapes);
        int cardW = dpToPx(90), cardH = dpToPx(110), margin = dpToPx(8);

        for (int i = 0; i < SHAPE_LABELS.length; i++) {
            final int shapeId = SHAPE_IDS[i];

            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(android.view.Gravity.CENTER);
            card.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(14));
            bg.setColor(boardView.getBoardShape() == shapeId
                    ? 0xFF111111 : 0xFFF5F5F5);
            card.setBackground(bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardW, cardH);
            lp.setMargins(margin, margin, margin, margin);
            card.setLayoutParams(lp);

            // Mini shape preview (just a colored circle/rect for now)
            View preview = new View(requireContext());
            int previewSize = dpToPx(44);
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(previewSize, previewSize);
            preview.setLayoutParams(plp);
            GradientDrawable previewBg = new GradientDrawable();
            previewBg.setShape(GradientDrawable.OVAL);
            previewBg.setColor(boardView.getBoardShape() == shapeId
                    ? 0xFFFFFFFF : 0xFF3B82F6);
            preview.setBackground(previewBg);

            TextView label = new TextView(requireContext());
            label.setText(SHAPE_LABELS[i]);
            label.setTextSize(11f);
            label.setTextColor(boardView.getBoardShape() == shapeId
                    ? 0xFFFFFFFF : 0xFF0A0A0A);
            label.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = dpToPx(6);
            label.setLayoutParams(tlp);

            card.addView(preview);
            card.addView(label);
            ll.addView(card);

            card.setOnClickListener(v -> {
                boardView.setShape(shapeId);
                refreshShapeCards(ll);
            });
        }
    }

    private void refreshShapeCards(LinearLayout ll) {
        for (int i = 0; i < ll.getChildCount(); i++) {
            LinearLayout card = (LinearLayout) ll.getChildAt(i);
            boolean selected = (SHAPE_IDS[i] == boardView.getBoardShape());

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(14));
            bg.setColor(selected ? 0xFF111111 : 0xFFF5F5F5);
            card.setBackground(bg);

            View preview = (View) card.getChildAt(0);
            GradientDrawable previewBg = new GradientDrawable();
            previewBg.setShape(GradientDrawable.OVAL);
            previewBg.setColor(selected ? 0xFFFFFFFF : 0xFF3B82F6);
            preview.setBackground(previewBg);

            TextView label = (TextView) card.getChildAt(1);
            label.setTextColor(selected ? 0xFFFFFFFF : 0xFF0A0A0A);
        }
    }

    // ── Color palette ────────────────────────────────────────────────────────

    private void populateColorPalette(LinearLayout ll, boolean isDrawPalette) {
        int size = dpToPx(38), margin = dpToPx(5);
        for (int color : PALETTE) {
            View swatch = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, margin, margin, margin);
            swatch.setLayoutParams(lp);

            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(color);
            if (color == 0xFFFFFFFF || color == Color.WHITE) {
                d.setStroke(dpToPx(2), 0xFFDDDDDD);
            }
            swatch.setBackground(d);

            final int c = color;
            swatch.setOnClickListener(v -> {
                if (isDrawPalette) {
                    boardView.setDrawColor(c);
                } else {
                    applyColorToTarget(c);
                }
            });
            ll.addView(swatch);
        }
    }

    private void applyColorToTarget(int color) {
        switch (colorTarget) {
            case 0: boardView.setFillColor(color);  break;
            case 1: boardView.setTruckColor(color); break;
            case 2: boardView.setWheelColor(color); break;
            case 3: boardView.setGripColor(color);  break;
        }
    }

    // ── Colors panel ─────────────────────────────────────────────────────────

    private void setupColorsPanel() {
        btnTargetDeck.setOnClickListener(v   -> selectColorTarget(0));
        btnTargetTrucks.setOnClickListener(v -> selectColorTarget(1));
        btnTargetWheels.setOnClickListener(v -> selectColorTarget(2));
        btnTargetGrip.setOnClickListener(v   -> selectColorTarget(3));

        btnGripNone.setOnClickListener(v    -> selectGripStyle(BoardView.GRIP_NONE));
        btnGripBlack.setOnClickListener(v   -> selectGripStyle(BoardView.GRIP_BLACK));
        btnGripColored.setOnClickListener(v -> selectGripStyle(BoardView.GRIP_COLORED));
    }

    private void selectColorTarget(int target) {
        colorTarget = target;
        styleTargetButtons(target);
        View scroll = rootView.findViewById(R.id.color_palette_scroll);
        if (target == 3) {
            rowGripStyle.setVisibility(View.VISIBLE);
            boolean showPalette = boardView.getGripStyle() == BoardView.GRIP_COLORED;
            scroll.setVisibility(showPalette ? View.VISIBLE : View.GONE);
        } else {
            rowGripStyle.setVisibility(View.GONE);
            scroll.setVisibility(View.VISIBLE);
        }
    }

    private void styleTargetButtons(int active) {
        TextView[] btns = { btnTargetDeck, btnTargetTrucks, btnTargetWheels, btnTargetGrip };
        for (int i = 0; i < btns.length; i++) {
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(8));
            if (i == active) {
                bg.setColor(0xFF111111);
                btns[i].setTextColor(0xFFFFFFFF);
            } else {
                bg.setColor(0xFFEEEEEE);
                btns[i].setTextColor(0xFF0A0A0A);
            }
            btns[i].setBackground(bg);
        }
    }

    private void selectGripStyle(int style) {
        boardView.setGripStyle(style);
        TextView[] btns = { btnGripNone, btnGripBlack, btnGripColored };
        int[] styles = { BoardView.GRIP_NONE, BoardView.GRIP_BLACK, BoardView.GRIP_COLORED };
        for (int i = 0; i < btns.length; i++) {
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(8));
            if (styles[i] == style) {
                bg.setColor(0xFF111111);
                btns[i].setTextColor(0xFFFFFFFF);
            } else {
                bg.setColor(0xFFEEEEEE);
                btns[i].setTextColor(0xFF0A0A0A);
            }
            btns[i].setBackground(bg);
        }
        // Show palette for colored grip
        View scroll = rootView.findViewById(R.id.color_palette_scroll);
        scroll.setVisibility(style == BoardView.GRIP_COLORED ? View.VISIBLE : View.GONE);
    }

    // ── Stickers panel ───────────────────────────────────────────────────────

    private void populateStickerGrid() {
        int cellSize = dpToPx(56), margin = dpToPx(4);

        for (int i = 0; i < STICKER_EMOJIS.length; i++) {
            final int type = STICKER_TYPES[i];

            TextView btn = new TextView(requireContext());
            btn.setText(STICKER_EMOJIS[i]);
            btn.setTextSize(22f);
            btn.setGravity(android.view.Gravity.CENTER);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(12));
            bg.setColor(0xFFF5F5F5);
            btn.setBackground(bg);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cellSize;
            lp.height = cellSize;
            lp.setMargins(margin, margin, margin, margin);
            btn.setLayoutParams(lp);
            btn.setClickable(true);
            btn.setFocusable(true);

            btn.setOnClickListener(v -> {
                boardView.setStickerType(type);
                boardView.setMode(BoardView.MODE_STICKER);
                highlightStickerBtn(btn);
            });

            gridStickers.addView(btn);
        }
        // Select first by default
        boardView.setStickerType(BoardView.STICKER_STAR);
    }

    private void highlightStickerBtn(TextView selected) {
        for (int i = 0; i < gridStickers.getChildCount(); i++) {
            TextView btn = (TextView) gridStickers.getChildAt(i);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(12));
            bg.setColor(btn == selected ? 0xFF111111 : 0xFFF5F5F5);
            btn.setBackground(bg);
        }
    }

    private void setupStickersPanel() {
        boardView.setStickerSize(30f);

        btnSizeS.setOnClickListener(v -> { boardView.setStickerSize(18f); highlightSize(btnSizeS); });
        btnSizeM.setOnClickListener(v -> { boardView.setStickerSize(30f); highlightSize(btnSizeM); });
        btnSizeL.setOnClickListener(v -> { boardView.setStickerSize(46f); highlightSize(btnSizeL); });
    }

    private void highlightSize(TextView selected) {
        for (TextView btn : new TextView[]{ btnSizeS, btnSizeM, btnSizeL }) {
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(8));
            bg.setColor(btn == selected ? 0xFF111111 : 0xFFEEEEEE);
            btn.setBackground(bg);
            btn.setTextColor(btn == selected ? 0xFFFFFFFF : 0xFF0A0A0A);
        }
    }

    // ── Draw panel ───────────────────────────────────────────────────────────

    private View rootView; // held so setup methods can find views during onCreateView

    private void setupDrawPanel() {
        boardView.setDrawColor(Color.WHITE);
        boardView.setBrushSize(8f);

        btnBrushS.setOnClickListener(v -> { boardView.setBrushSize(4f);  highlightBrush(btnBrushS); });
        btnBrushM.setOnClickListener(v -> { boardView.setBrushSize(8f);  highlightBrush(btnBrushM); });
        btnBrushL.setOnClickListener(v -> { boardView.setBrushSize(14f); highlightBrush(btnBrushL); });

        rootView.findViewById(R.id.btn_undo).setOnClickListener(v -> boardView.undo());
        rootView.findViewById(R.id.btn_clear_draw).setOnClickListener(v -> boardView.clear());
    }

    private void highlightBrush(TextView selected) {
        for (TextView btn : new TextView[]{ btnBrushS, btnBrushM, btnBrushL }) {
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(8));
            bg.setColor(btn == selected ? 0xFF111111 : 0xFFEEEEEE);
            btn.setBackground(bg);
            btn.setTextColor(btn == selected ? 0xFFFFFFFF : 0xFF0A0A0A);
        }
    }

    // ── Tab switching ────────────────────────────────────────────────────────

    private void setupTabs() {
        tabShape.setOnClickListener(v    -> switchTab(0));
        tabColors.setOnClickListener(v   -> switchTab(1));
        tabStickers.setOnClickListener(v -> switchTab(2));
        tabDraw.setOnClickListener(v     -> switchTab(3));
        switchTab(0); // default
    }

    private void switchTab(int index) {
        // Board mode
        if (index == 2) boardView.setMode(BoardView.MODE_STICKER);
        else            boardView.setMode(BoardView.MODE_DRAW);

        // Panel visibility
        panelShape.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        panelColors.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        panelStickers.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        panelDraw.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        // Tab text styling
        TextView[] tabs = { tabShape, tabColors, tabStickers, tabDraw };
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setTextColor(i == index ? 0xFF0A0A0A : 0xFF9E9E9E);
        }

        // Move indicator to the selected tab using post so layout is ready
        final TextView activeTab = tabs[index];
        tabIndicator.post(() -> {
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                            tabIndicator.getLayoutParams();
            lp.startToStart = activeTab.getId();
            lp.endToEnd     = activeTab.getId();
            tabIndicator.setLayoutParams(lp);
        });

        // Initial Deck target highlight on Colors tab
        if (index == 1) {
            selectColorTarget(colorTarget);
        }
        // Initial brush highlight on Draw tab
        if (index == 3) {
            highlightBrush(btnBrushM);
        }
        // Default sticker size on Stickers tab
        if (index == 2) {
            highlightSize(btnSizeM);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
