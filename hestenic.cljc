
;;
;;
;;

(ns hestenic
  "the superabsorbent and lint-free world of geometric algebra (and perhaps,
   in the fullness of time, onward to geo'tric calculus)")

;;
;; The approach we'll use here is protocol-rich... or -heavy, depending
;; on your taste. But in general we wish to represent elements in GA
;; at a few natural levels: weighted basis blades ('Bladoid');
;; collections of a number of Bladoids of uniform grade ('Gradeling'),
;; sorted lexicographically on basis; collections of Gradelings of
;; heterogeneous grade ('MV', for multivector), sorted on grade; and
;; vector itself ('Vect'). In addition, for the moment we have a special
;; class to represent the zero element ('ZeroElement'), which is identical
;; for all grades. These different representations (or GA elements) are
;; called here in the source file 'rungs'.
;;
;; The protocol immediately following is the (presently malnourished but
;; still growing) set of canonical operations applicable to each of the
;; aforenamed elements. It might be argued that the final protocol in
;; this section ('IScamperUpAndDownLadder') could reasonably be merged
;; with the basic protocol ('IHestenic'), but from the current insomniac
;; perspective it seems cognitively useful to keep 'em separate.
;;
;; We extend many of these protocols too to java.lang.Number to revel
;; in automatic interop of existing numeric types with the GA system.
;; Thanks, lisp dialect!
;;
;; This implementation is knowably inefficient, and that to a ridiculous
;; degree; but it intends to articulate via code the mechanics of GA
;; calculations in a clean and efficient way. We assume that this one
;; will be followed by a far more parsimonious implementation.
;;
;; Note, more pleasingly, that the implementation is effectively
;; dimension-independent. None of the representations requires or
;; indeed expresses any particular dimension -- if you wedge e0 and
;; e1 and e2, then you're "in" three dimensions; but if you then
;; wedge that with e4, well, you're in 5D. There are of course
;; operations that require asserting a dimension: at the moment,
;; only the pseudoscalar (and derived bits, like dualization) takes
;; a 'dim' argument.
;;


(defprotocol IHestenic
  ;; first the algebraic operators
  (scl [this s]
    "the GA element scaled by 's', which must be a real")
  (sum [this otha]
    "the GA elements' sum.")
  (neg [this]
    "the GA element's additive inverse.")
  (rev [this]
    "the GA element's reversion.")
  (gri [this]
    "the GA element's grade involution.")
  (dif [this otha]
    "the second GA element subtracted from the first.")
  (prd[this otha]
    "the two GA elements' geometric product. This is the big one -- the
     whole of the algebra is generated by / derives from this most fundamental
     operation.")
  (inv [this]
    "the GA element's multiplicative inverse with respect to the geometric
     product. Note benefully that not all elements are invertible... though
     more are than the present implementation accounts for." )
  (quo [this otha]
    "the geometric product of the first GA element and the multiplicative
     inverse of the second... presuming of course that this latter exists.")
  (dot [this otha]
    "the two GA elements' inner product in the symmetric take on such
     things: the abs(k-j) grade part of the geometric product of the
     elements (assuming they are respectively of grades j and k), or
     the distributively re-collected sum of all the pairwise inner
     products between the bi-exploded components of multi-grade
     elements.")
  (wdg [this otha]
    "the two GA elements' outer product. That is to say: the j+k grade part
     of the two elements' geometric product, if they're both single-grade
     entities, or the distributively re-collected sum of the pairwise
     outer products of all bi-exploded components of the two when multi-grade
     entities.")
  (lcn [this otha]
    "the two GA elements' left contraction: like the symmetric inner product
     ('dot'), but the k-j grade part, which, see, is zero whenever j>k,
     i.e. when the first element if of higher grade than the second.")
  (rcn [this otha]
    "the two GA elements' right contraction: the grade j-k part of the
     geometric product, which, per the definition of grade selection,
     is zero for k>j.")
  (hip [this otha]
    "the two GA elements' Hestenes inner product, which is identical to
     the conventional dot product unless either argument is a scalar
     (i.e. of grade zero), whereupon the product vanishes.")

  ;; and now the interrogatives
  (eq? [this otha]
    "whether the two GA elements are the same mathematial object.")
  (scalar? [this]
    "whether the thing is of grade zero, i.e. a real.")
  (invertible? [this]
    "whether the GA eleement has an inverse.")
  (monograde? [this]
    "whether the GA eleement comprises a single grade (i.e. is
    'homogeneous', as they say).")

  ;; then finally grade query and selector
  (grade [this]
    "for all GA elements except the multivector, answers that element's
     singular grade (as an integer). For a multivector, a list of
     the grades therein.")
  (grades [this]
    "answers a list of the zero or more grades contained in the GA element")
  (grp [this n]
    "the GA element's grade 'n' part."))


(comment  ;; wrapping a compacted (i.e. docstring-free) version of IHestenic
          ;; thusly for visual convenience
(defprotocol IHestenic
  (scl [this s])
  (sum [this otha])
  (neg [this])
  (rev [this])
  (gri [this])
  (dif [this otha])
  (prd [this otha])
  (inv [this])
  (quo [this otha])
  (dot [this otha])
  (wdg [this otha])
  (lcn [this otha])
  (rcn [this otha])
  (hip [this otha])
  (eq? [this otha])
  (scalar? [this])
  (invertible? [this])
  (monograde? [this])
  (grade [this])
  (grades [this])
  (grp [this n]))
)

(defprotocol IAsBladoid
  (asBladoid [this]))

(defprotocol IAsGradeling
  (asGradeling [this]))

(defprotocol IAsMV
  (asMV [this]))

(defprotocol IScamperUpAndDownLadder
  (spankOutNothingness [this])
  (upRungify [this])
  (downRungify [this]))


;;
;; the following composition should reduce any GA object to its 'minimal'
;; representation.
;;

(defn- accordion-down [hest-elmt]
  (downRungify
   (spankOutNothingness
    hest-elmt)))


;;
;; a metric for the algebra: oh you betcha! or, really, 'metrics', since
;; the idea here is that the dynamic var following can be mutated (or
;; temporarily binding'd) to define afresh the metric...
;;
;; the machinery defaults to a metric of positive unity whenever there
;; isn't an entry for the index sought (which includes the resting-state
;; case of the metric parameter's being itself nil).
;;
;; for a temporary whiff of a metric different from the one currently
;; in play, you'll want to do e.g.
;;
;; (with-metric hestenic-minkowski-3 (prd e0 e012)) ==> #b(-1.0 [1 2])
;;
;; ... whereas if you want to durably change the metric it'll be
;;
;; (set-metric! hestenic-minkowski-3)
;;

(def ^:dynamic *hestenic-metric* nil)

(defn metric-for-basis-index [ind]
  (let [emm (get *hestenic-metric* ind)]
    (if (nil? emm)  1  emm)))

(defn cumu-metric-factor-for-bases [basis-arr]
  (reduce (fn [acc ind] (* acc (metric-for-basis-index ind)))
          1  basis-arr))

(defmacro with-metric
  [tric & formses]
  `(binding [*hestenic-metric* ~tric]
     ~@formses))

(defn set-metric! [tric]
  (alter-var-root (var *hestenic-metric*)
                  (fn [_] tric)))

(defn reset-metric! [] (set-metric! nil))


(def hestenic-minkowski-3 [-1 -1 -1 +1])


;;
;; well, then: here goes. following are the protocol definitions for the
;; GA element representations on clojuric offer here: Bladoid, Gradeling,
;; MV, Vect, and ZeroElement.
;;

;;
;; all things Bladoid, don't you know. well, not all. in any event, one of
;; these is a weighted (scaled) canonical basis element of the GA. does not
;; for the moment presuppose any particular dimension. also does not afford
;; configurable metrics. not yet. but soon.
;;

(defprotocol IBladoid
  (coef [this])
  (basis [this])
  (square-sign [this])
  (same-basis? [this otha]))


(deftype Bladoid [f_coef f_basis]
  IBladoid
  (coef [_] f_coef)
  (basis [_] f_basis)
  (square-sign [_]
    (let [gr (count f_basis)
          flormp (/ (* gr (- gr 1)) 2)]
      (if (even? flormp) +1 -1)))
  (same-basis? [this otha]
    (= (basis this) (basis otha)))

  Object
  (toString [_] (str f_coef ":[" (clojure.string/join f_basis) "]")))


(defn bladoid
  ([]
   (Bladoid. 1.0 []))
  ([c_or_b]
   (if (sequential? c_or_b)
     (Bladoid. 1.0 (vec (sort c_or_b)))
     (Bladoid. c_or_b [])))
  ([c b]
   (Bladoid. c (vec (sort b)))))  ;; force sort to give back the same type.


(defmethod print-method Bladoid [^Bladoid doid ^java.io.Writer w]
  (.write w (str "#b(" (coef doid) " ["
                 (clojure.string/join " " (basis doid))
                 "])")))

(defn parse-bladoid [[wgt bss]]
  (bladoid wgt bss))


(defn- collapse [doids]
  (map (partial reduce sum)
       (partition-by basis doids)))


(defn- confirm-grades!
  ([blds]  ;; ar/1: infer grade from first bladoid
   (if (empty? blds)
     blds
     (confirm-grades! blds (grade (first blds)))))
  ([blds gra]  ;; ar/2: with grade specified
   (if (not (empty? blds))
     (doseq [bld blds]
       (if (not= gra (grade bld))
         (throw (Exception. (str "incompatible element: "
                                 bld " wants to be of grade " gra))))))
   blds))

(defn- basis-prod [bas1 bas2]
  (let [ess1 (set bas1)
        ess2 (set bas2)]
    (vec (sort
          (clojure.set/difference (clojure.set/union ess1 ess2)
                                  (clojure.set/intersection ess1 ess2))))))

(defn- scoot-count [val rseq]
  (loop [scoo 0
         arr rseq]
    (if (or (empty? arr)
            (<= val (first arr)))
      scoo
      (recur (inc scoo) (rest arr)))))

(defn- order-swap-count [basl basr]
  (loop [cnt 0
         lind (dec (count basl))]
    (if (< lind 0)
      cnt
      (let [lval (nth basl lind)
            sc (scoot-count lval basr)]
        (if (= sc 0)
          cnt
          (recur (+ cnt sc) (dec lind)))))))

(defn- order-swap-parity [basl basr]
  (if (even? (order-swap-count basl basr)) +1 -1))

(defn- metric-factor [basl basr]
  (cumu-metric-factor-for-bases
   (map first
        (filter (fn [basind] (> (count basind) 1))
                (partition-by identity (sort (concat basl basr)))))))

(defn- sort-on-bases [bldds]
  (vec (sort-by basis bldds)))

(defn- sort-on-grades [elmnts]
  (vec (sort-by grade elmnts)))

(defn- same-grade? [this otha]
  (= (grade this) (grade otha)))

(defn- different-rung? [this otha]
  (not (instance? (class this) otha)))


;;
;; A Gradeling is a bevy of Bladoids of the same grade. It may or may not be
;; itself a blade (the 'not' part becomes a possibility for dimensions above
;; three, which offers unfactorable disgraces like e01 + e12).
;;

(defprotocol IGradeling
  (bladoids [this])
  (bladoid-with-basis [this b])
  (sole-grade [this]))


(deftype Gradeling [f_grade f_bladoids]
  IGradeling
  (bladoids [_] f_bladoids)
  (bladoid-with-basis [this b]
    (first (filter (fn [el] (= (basis el) b))
                   f_bladoids)))
  (sole-grade [_] f_grade)

  Object
  (toString [_] (str "g" f_grade "("
                     (clojure.string/join " " f_bladoids)
                     ")")))


;; singin' them old-timey singletunes:

(def the-empty-gradeling (Gradeling. -1 []))

(defn gradeling [grade-or-bladoids]
  (if (integer? grade-or-bladoids)
    (Gradeling. grade-or-bladoids [])
    (let [gra (if (empty? grade-or-bladoids)
                -1
                (grade (first grade-or-bladoids)))]
      (Gradeling. gra (collapse
                       (sort-on-bases
                        (confirm-grades! grade-or-bladoids gra)))))))

(defmethod print-method Gradeling [^Gradeling grdl ^java.io.Writer w]
  (.write w (str "#g" (comment (grade grdl)) "("
                 (clojure.string/join " " (bladoids grdl))
                 ")")))


;;
;; Multivector is, mirabile dictu, a multivector: an collection of zero or
;; more Gradelings: which is to say an arbitrary collection of Bladoids
;;

(defprotocol IMV
  (gradelings [this])
  (gradeling-of-grade [this gra]))

(deftype MV [f_gradelings]
  IMV
  (gradelings [_] f_gradelings)
  (gradeling-of-grade [this gra]
    (first (filter (fn [grdl] (= (grade grdl) gra))
                   f_gradelings)))

  Object
  (toString [_] (str "MV["
                     (clojure.string/join " " f_gradelings)
                     "]")))

(defmethod print-method MV [^MV emvy ^java.io.Writer w]
  (.write w (str "#MV" "["
                 (clojure.string/join " " (gradelings emvy))
                 "]")))

(def the-empty-mv (MV. []))

(defn mv [gradels]
  (MV. (sort-on-grades gradels)))


;;
;; Vect is, of course, a vector (meaning -- yes! -- a real geometric,
;; mathematical vector, for Zoroaster's sake; not an 'array', per
;; lexical cretinism).
;;
;; In a strict sense this is a redundant type-entity, since one of these
;; could always be represented as a Gradeling of grade one. But because
;; vectors have a vaunted position in GA -- they are rightly the generators
;; of the whole algebra, y' see -- it's very much worth representing them
;; explicitly.
;;

(defprotocol IVect
  (coefs [this]))

(deftype Vect [f_coefs]
  IVect
  (coefs [_] f_coefs)

  Object
  (toString [_] (str "v(" (clojure.string/join " " f_coefs) ")")))


(defn vect [& cs] (Vect. (vec cs)))

(defmethod print-method Vect [^Vect veee ^java.io.Writer w]
  (.write w (str "#v" "("
                 (clojure.string/join " " (coefs veee))
                 ")")))

(defn parse-vect [coefficients-aplenty]
  (apply vect coefficients-aplenty))


;;
;; herewith the special representation for that certain lack of anything.
;;
;; a philosophical quandary whether we're better off with or without
;; this one; the interop with built-in numbers, including of course
;; 0 (& 0.0 et al.), is so pleasing and frictionless that we could
;; ditch this creature.
;;

(deftype ZeroElement []
  IScamperUpAndDownLadder
  (spankOutNothingness [this]
    this)
  (upRungify [_]
    (Bladoid. 0 []))
  (downRungify [_] 0)

  IHestenic
  (scl [this s] this)
  (sum [this otha] otha)
  (neg [this] this)
  (rev [this] this)
  (gri [this] this)
  (dif [this otha] (neg otha))
  (prd [this otha] this)
  (inv [this] (/ 1 0))
  (quo [this otha]
    (let [invvy (inv otha)]
      this))
  (dot [this otha] this)
  (wdg [this otha] this)
  (lcn [this otha] this)
  (rcn [this otha] this)
  (hip [this otha] this)
  (eq? [this otha]
    (or (instance? (class this) otha)
        (and (number? otha) (zero? otha))))
  (scalar? [this] true)  ;; well, actually a conundrum
  (invertible? [this] false)
  (monograde? [this] true)  ;; also troublingly conundrifying...
  (grade [this] nil)  ;; the zero element is all grades and none, you see.
  (grades [this] '())
  (grp [this n] this))


;; the incomparable nullful stylings of sarah and the singletones:

(def the-zero-element (ZeroElement.))


;;
;; a shadowy cabal of utility-interior mathy bits.
;;

;; first: plusularly inclined combinifications

(defn- gradeling-absorb-bladoid [l-grdl r-doid]
  (let [gra (grade l-grdl)]
    (if (= gra -1)
      (Gradeling. (grade r-doid) (vector r-doid))
      (if (not= gra (grade r-doid))
        (throw (Exception. (str "can't absorb grade" (grade r-doid)
                                " blade into grade " gra
                                "gradeling...")))
        (if (bladoid-with-basis l-grdl (basis r-doid))
          (Gradeling. gra (vec
                           (map (fn [l-doid]
                                  (if (same-basis? l-doid r-doid)
                                    (Bladoid. (+ (coef l-doid) (coef r-doid))
                                              (basis r-doid))
                                    l-doid))
                                (bladoids l-grdl))))
          (Gradeling. gra (sort-on-bases
                           (conj (bladoids l-grdl) r-doid))))))))

(defn- gradeling-absorb-gradeling [l-grdl r-grdl]
  (reduce gradeling-absorb-bladoid
          l-grdl (bladoids r-grdl)))

(defn- mv-absorb-gradeling [l-emvy r-grdl]
  (if (gradeling-of-grade l-emvy (grade r-grdl))
    (MV. (map (fn [l-grdl]
                (if (same-grade? l-grdl r-grdl)
                  (gradeling-absorb-gradeling l-grdl r-grdl)
                  l-grdl))
              (gradelings l-emvy)))
    (MV. (sort-on-grades (conj (gradelings l-emvy) r-grdl)))))

(defn- mv-absorb-bladoid [l-emvy r-doid]
  (mv-absorb-gradeling l-emvy (asGradeling r-doid)))

(defn- mv-absorb-mv [l-emvy r-emvy]
  (reduce mv-absorb-gradeling
          l-emvy (gradelings r-emvy)))


;; and then: timesistic manipulationizings

(defn- bladoid-mult-bladoid [l-doid r-doid]
  (let [basl (basis l-doid)
        basr (basis r-doid)]
    (Bladoid. (* (coef l-doid) (coef r-doid)
                 (order-swap-parity basl basr)
                 (metric-factor basl basr))
              (basis-prod basl basr))))

(defn- bladoid-mult-gradeling [l-doid r-grdl]
  (reduce (fn [acc-emvy r-doid]
            (mv-absorb-bladoid acc-emvy (prd l-doid r-doid)))
          the-empty-mv
          (bladoids r-grdl)))

(defn- gradeling-mult-gradeling [l-grdl r-grdl]
  (reduce (fn [acc-emvy doid] (mv-absorb-mv
                                acc-emvy
                                (bladoid-mult-gradeling doid r-grdl)))
          the-empty-mv
          (bladoids l-grdl)))

(defn- mv-mult-gradeling [l-emvy r-grdl]
  (reduce (fn [acc-mv l-grdl]
            (mv-absorb-mv
             acc-mv
             (gradeling-mult-gradeling l-grdl r-grdl)))
          the-empty-mv
          (gradelings l-emvy)))

(defn- mv-map-biexploded-gradelings-and-sum [funq l-emvy r-emvy]
  (reduce mv-absorb-mv
          the-empty-mv
          (for [l-grdl (gradelings l-emvy)
                r-grdl (gradelings r-emvy)]
            (asMV (funq l-grdl r-grdl)))))


;;
;; and, finally, the glorious salon des refuses
;;

(defn reversion-parity [gr]
  (if (even? (/ (* gr (- gr 1)) 2))  +1  -1))

(defn grade-involution-parity [gr]
  (if (even? gr)  +1  -1))


(defn pseudoscalar [dim]
  (Bladoid. 1 (vec (range dim))))

(defn inverse-pseudoscalar [dim]
  (Bladoid. (reversion-parity dim) (vec (range dim))))


(defn dua [hest-elmt dim]
  (prd hest-elmt (inverse-pseudoscalar dim)))

(defn scp [hest-elmt otha]
  (accordion-down (grp (prd hest-elmt otha) 0)))


;;
;; rung conversions: one, yea, unto another.
;;

(extend-type Bladoid
  IAsBladoid
  (asBladoid [this]
    this)

  IAsGradeling
  (asGradeling [this]
    (Gradeling. (grade this) (vector this)))

  IAsMV
  (asMV [this]
    (asMV (asGradeling this))))


(extend-type Gradeling
  IAsGradeling
  (asGradeling [this]
    this)

  IAsMV
  (asMV [this]
    (MV. [this])))


(extend-type MV
  IAsMV
  (asMV [this]
    this))


(extend-type Vect
  IAsGradeling
  (asGradeling [this]
    (Gradeling. 1 (vec (filter (fn [bl] (not= 0 (coef bl)))
                               (map-indexed
                                (fn [ind cf] (bladoid cf (vector ind)))
                                (coefs this))))))

  IAsMV
  (asMV [this]
    (asMV (asGradeling this))))


(extend-type ZeroElement
  IAsBladoid
  (asBladoid [_]
    (Bladoid. 0 []))

  IAsGradeling
  (asGradeling [this]
    (asGradeling (asBladoid this)))

  IAsMV
  (asMV [this]
    (asMV (asBladoid this))))


;;
;; simplification (and complexification) of species
;;

(extend-type Bladoid
  IScamperUpAndDownLadder
  (spankOutNothingness [this]
    (if (zero? (coef this))
      0
      this))
  (upRungify [this]
    (asGradeling this))
  (downRungify [this]
    (if (empty? (basis this))
      (coef this)
      this)))

(extend-type Gradeling
  IScamperUpAndDownLadder
  (spankOutNothingness [this]
    (let [spnkd (filter
                 (fn [bl] (not (zero? (coef bl))))
                 (bladoids this))]
      (if (empty? spnkd)
        0
        (Gradeling. (grade this) spnkd))))
  (upRungify [this]
    (asMV this))
  (downRungify [this]
    (let [bls (bladoids this)]
      (if (= 1 (count bls))
        (downRungify (first bls))
        this))))

(extend-type MV
  IScamperUpAndDownLadder
  (spankOutNothingness [this]
    (let [nullplop (map
                    spankOutNothingness
                    (gradelings this))
          spnkd (filter
                 (fn [grdl] (not (number? grdl)))
                 nullplop)]
      (if (empty? spnkd)
        0
        (MV. spnkd))))
  (upRungify [this]
    this)
  (downRungify [this]
    (let [grdls (gradelings this)]
      (if (= 1 (count grdls))
        (downRungify (first grdls))
        this))))

(extend-type Vect
  IScamperUpAndDownLadder
  (spankOutNothingness [this]
    (if (every? zero? (coefs this))
      0
      this))
  (upRungify [this]
    (asGradeling this))
  (downRungify [this]
    this))


;;
;; now for the good stuff.
;;

(extend-type Bladoid
  IHestenic
  (scl [this s]
    (Bladoid. (* (coef this) s) (basis this)))
  (sum [this otha]
    (if (different-rung? this otha)
      (sum (asMV this) (asMV otha))
      (accordion-down
       (if (same-basis? this otha)
         (Bladoid. (+ (coef this) (coef otha)) (basis this))
         (if (same-grade? this otha)
           (Gradeling. (grade this)
                       (sort-on-bases (vector this otha)))
           (MV. (sort-on-grades (vector (asGradeling this)
                                        (asGradeling otha)))))))))
  (neg [this] (scl this -1))
  (rev [this]
    (if (pos? (reversion-parity (grade this)))
      this
      (neg this)))
  (gri [this]
    (if (pos? (grade-involution-parity (grade this)))
      this
      (neg this)))
  (dif [this otha] (sum this (neg otha)))
  (prd [this otha]
    (if (different-rung? this otha)
      (if (number? otha)
        (scl this otha)
        (prd (asMV this) (asMV otha)))
      (accordion-down
       (bladoid-mult-bladoid this otha))))
  (inv [this]
    (let [bas (basis this)]
      (Bladoid. (* (/ 1 (coef this) (cumu-metric-factor-for-bases bas))
                   (square-sign this))
                (basis this))))
  (quo [this otha] (prd this (inv otha)))
  (dot [this otha]
    (if (different-rung? this otha)
      (dot (asMV this) (asMV otha))
      (grp (prd this otha)
           (Math/abs (- (grade this) (grade otha))))))
  (wdg [this otha]
    (if (different-rung? this otha)
      (wdg (asMV this) (asMV otha))
      (grp (prd this otha)
           (+ (grade this) (grade otha)))))
  (lcn [this otha]
    (if (different-rung? this otha)
      (lcn (asMV this) (asMV otha))
      (let [out-grade (- (grade otha) (grade this))]
        (if (neg? out-grade)
          the-zero-element
          (grp (prd this otha) out-grade)))))
  (rcn [this otha]
    (if (different-rung? this otha)
      (rcn (asMV this) (asMV otha))
      (let [out-grade (- (grade this) (grade otha))]
        (if (neg? out-grade)
          the-zero-element
          (grp (prd this otha) out-grade)))))
  (hip [this otha]
    (if (or (scalar? this) (scalar? otha))
      the-zero-element
      (dot this otha)))
  (eq? [this otha]
    (if (different-rung? this otha)
      (eq? (asMV this) otha)
      (and (= (coef this) (coef otha))
           (= (basis this) (basis otha)))))
  (scalar? [this]
    (= 0 (grade this)))
  (invertible? [this]
    (not (zero? (dot this this))))
  (monograde? [this] true)
  (grade [this] (count (basis this)))
  (grades [this] (list (count (basis this))))
  (grp [this n]
    (if (= (grade this) n)
      this
      the-zero-element)))


(extend-type Gradeling
  IHestenic
  (scl [this s]
    (Gradeling. (grade this) (map (fn [bld] (scl bld s))
                                  (bladoids this))))
  (sum [this otha]
    (if (different-rung? this otha)
      (sum (asMV this) (asMV otha))
      (accordion-down
       (if (same-grade? this otha)
         (gradeling-absorb-gradeling this otha)
         (MV. (sort-on-grades (vector this otha)))))))
  (neg [this] (Gradeling. (grade this)
                          (map (fn [bl] (neg bl)) (bladoids this))))
  (rev [this]
    (if (pos? (reversion-parity (grade this)))
      this
      (neg this)))
  (gri [this]
    (if (pos? (grade-involution-parity (grade this)))
      this
      (neg this)))
  (dif [this otha] (sum this (neg otha)))
  (prd [this otha]
    (if (different-rung? this otha)
      (if (number? otha)
        (scl this otha)
        (prd (asMV this) (asMV otha)))
      (accordion-down
       (gradeling-mult-gradeling this otha))))
  (inv [this]
    (let [s (/ 1.0 (scp this this))]
      (scl this s)))
  (quo [this otha] (prd this (inv otha)))
  (dot [this otha]
    (if (different-rung? this otha)
      (dot (asMV this) (asMV otha))
      (grp (prd this otha)
           (Math/abs (- (grade this) (grade otha))))))
  (wdg [this otha]
    (if (different-rung? this otha)
      (wdg (asMV this) (asMV otha))
      (grp (prd this otha)
           (+ (grade this) (grade otha)))))
  (lcn [this otha]
    (if (different-rung? this otha)
      (lcn (asMV this) (asMV otha))
      (let [out-grade (- (grade otha) (grade this))]
        (if (neg? out-grade)
          the-zero-element
          (grp (prd this otha) out-grade)))))
  (rcn [this otha]
    (if (different-rung? this otha)
      (rcn (asMV this) (asMV otha))
      (let [out-grade (- (grade this) (grade otha))]
        (if (neg? out-grade)
          the-zero-element
          (grp (prd this otha) out-grade)))))
  (hip [this otha]
    (if (or (scalar? this) (scalar? otha))
      the-zero-element
      (dot this otha)))
  (eq? [this otha]
    (if (different-rung? this otha)
      (eq? (asMV this) otha)
      (and (= (grade this) (grade otha))
           (every? true?
                   (map eq? (bladoids this) (bladoids otha))))))
  (scalar? [this]
    (= 0 (grade this)))
  (invertible? [this]
    (not (zero? (dot this this))))
  (monograde? [this] true)
  (grade [this] (sole-grade this))
  (grades [this] (list (sole-grade this)))
  (grp [this n]
    (if (= (grade this) n)
      this
      the-zero-element)))



(defn- mv-bimap-via-gradelings [funq mvl mvr]
  (accordion-down
   (mv-map-biexploded-gradelings-and-sum
    funq mvl mvr)))

(extend-type MV
  IHestenic
  (scl [this s]
    (MV. (map (fn [grdl] (scl grdl s))
              (gradelings this))))
  (sum [this otha]
    (accordion-down
     (mv-absorb-mv this (asMV otha))))
  (neg [this]
    (MV. (map (fn [grdl] (neg grdl)) (gradelings this))))
  (rev [this]
    (reduce (fn [acc grdl] (mv-absorb-gradeling acc (rev grdl)))
            the-empty-mv
            (gradelings this)))
  (gri [this]
    (reduce (fn [acc grdl] (mv-absorb-gradeling acc (gri grdl)))
            the-empty-mv
            (gradelings this)))
  (dif [this otha]
    (sum this (neg otha)))
  (prd [this otha]
    (if (number? otha)
      (scl this otha)
      (mv-bimap-via-gradelings prd this (asMV otha))))
  (inv [this]
    (let [s (/ 1.0 (scp this this))]
      (scl this s)))
  (quo [this otha]
    (prd this (asMV (inv otha))))
  (dot [this otha]
    (mv-bimap-via-gradelings dot this (asMV otha)))
  (wdg [this otha]
    (mv-bimap-via-gradelings wdg this (asMV otha)))
  (lcn [this otha]
    (mv-bimap-via-gradelings lcn this (asMV otha)))
  (rcn [this otha]
    (mv-bimap-via-gradelings rcn this (asMV otha)))
  (hip [this otha]
    (if (or (scalar? this) (scalar? otha))
      the-zero-element
      (dot this otha)))
  (eq? [this otha]
    (every? true?
            (map eq? (gradelings this) (gradelings (asMV otha)))))
  (scalar? [this]
    (let [grdls (gradelings this)]
      (and (= 1 (count grdls))
           (= 0 (grade (first grdls))))))
  (invertible? [this]
    (not (zero? (scp this this))))
  (monograde? [this]
    (= 1 (count (gradelings this))))
  (grade [this] (grades this))
  (grades [this] (map grade (gradelings this)))
  (grp [this n]
    (let [grdl (filter (fn [g] (= (grade g) n))
                       (gradelings this))]
      (if (empty? grdl)
        the-zero-element
        (first grdl)))))


(extend-type Vect
  IHestenic
  (scl [this s]
    (Vect. (vec (map (fn [co] (* co s))
                     (coefs this)))))
  (sum [this otha]
    (if (different-rung? this otha)
      (sum (asMV this) (asMV otha))
      (accordion-down
       (Vect. (vec (map (fn [hoo hah] (+ hoo hah))
                        (coefs this) (coefs otha)))))))
  (neg [this]
    (Vect. (vec (map (fn [co] (- co))
                     (coefs this)))))
  (rev [this] this)
  (gri [this] (neg this))
  (dif [this otha]
    (sum this (neg otha)))
  (prd [this otha]
    (prd (asMV this) (asMV otha)))
  (inv [this]
    (let [sq (dot this this)]
      (scl this (/ 1.0 sq (cumu-metric-factor-for-bases
                           (range (grade this)))))))
  (quo [this otha]
    (prd this (inv otha)))
  (dot [this otha]
    (reduce +
            (map *
                 (coefs this) (coefs otha))))
  (wdg [this otha]
    (if (different-rung? this otha)
      (wdg (asMV this) (asMV otha))
      (grp (prd this otha) 2)))
  (lcn [this otha]
    (if (different-rung? this otha)
      (lcn (asMV this) otha)
      (dot this otha)))
  (rcn [this otha]
    (if (different-rung? this otha)
      (rcn (asMV this) otha)
      (dot this otha)))
  (hip [this otha]
    (if (scalar? otha)
      the-zero-element
      (dot this otha)))
  (eq? [this otha]
    (if (different-rung? this otha)
      (eq? (asMV this) otha)
      (let [thc (coefs this)
            otc (coefs otha)]
        (and (= (count thc) (count otc))
             (every? true? (map == thc otc))))))
  (scalar? [this] false)
  (invertible? [this]
    (not (zero? (dot this this))))
  (monograde? [this] true)
  (grade [_] 1)
  (grades [_] '(1))
  (grp [this n]
    (if (= n 1)
      this
      the-zero-element)))


;;
;; infect clojure/java's inbuilt numeric system with hestenicity
;;

(extend-type java.lang.Number
  IAsBladoid
  (asBladoid [this]
    (Bladoid. this []))

  IAsGradeling
  (asGradeling [this]
    (asGradeling (asBladoid this)))

  IAsMV
  (asMV [this]
    (asMV (asBladoid this)))

  IScamperUpAndDownLadder
  (spankOutNothingness [this]
    this)
  (upRungify [this]
    (Bladoid. this []))
  (downRungify [this]
    this)

  IHestenic
  (scl [this s]
    (* this s))
  (sum [this otha]
    (if (number? otha)
      (+ this otha)
      (sum (asBladoid this) otha)))
  (neg [this]
    (- this))
  (rev [this] this)
  (gri [this] this)
  (dif [this otha]
    (if (number? otha)
      (- this otha)
      (dif (asBladoid this) otha)))
  (prd [this otha]
    (if (number? otha)
      (* this otha)
      (scl otha this)))
  (inv [this]
    (/ 1.0 this))
  (quo [this otha]
    (prd this (inv otha)))
  (dot [this otha]
    (scl otha this))
  (wdg [this otha]
    (scl otha this))
  (lcn [this otha]
    (scl otha this))
  (rcn [this otha]
    (rcn (asMV this) (asMV otha)))
  (hip [this otha]
    the-zero-element)
  (eq? [this otha]
    (if-not (number? otha)
      (eq? (asMV this) otha)
      (== this otha)))
  (scalar? [this] true)
  (invertible? [this]
    (not (zero? this)))
  (monograde? [this] true)
  (grade [_] 0)
  (grades [_] '(0))
  (grp [this n]
    (if (= n 0)
      this
      the-zero-element)))


;;
;; prolixful synonymery
;;

(def scale scl)
;; can't really get any more summier: (def sum sum)
(def negative neg)
(def reversion rev)
(def grade-involution gri)
(def difference dif)
(def product prd)
(def inverse inv)
(def quotient quo)
(def inner-product dot)
(def wedge-product wdg)
(def left-contraction lcn)
(def right-contraction rcn)
(def hestenes-inner-product hip)

(def dual dua)
(def scalar-product scp)

(def grade-part grp)

;;
;; you know -- for kids.
;;

(def e0 (bladoid 1.0 [0]))
(def e1 (bladoid 1.0 [1]))
(def e2 (bladoid 1.0 [2]))
(def e01 (bladoid 1.0 [0 1]))
(def e02 (bladoid 1.0 [0 2]))
(def e12 (bladoid 1.0 [1 2]))
(def e012 (bladoid 1.0 [0 1 2]))
